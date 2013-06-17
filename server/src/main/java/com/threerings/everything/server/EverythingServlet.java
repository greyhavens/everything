//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import com.restfb.types.User;

import com.samskivert.servlet.util.CookieUtil;
import com.samskivert.util.Calendars;
import com.samskivert.util.Comparators;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.user.ExternalAuther;
import com.threerings.user.OOOUser;

import com.threerings.app.client.ServiceException;
import com.threerings.app.server.UserLogic;
import com.threerings.facebook.servlet.FacebookConfig;

import com.threerings.everything.data.Card;
import com.threerings.everything.data.CategoryComment;
import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.News;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.PlayerStats;
import com.threerings.everything.data.SessionData;
import com.threerings.everything.data.ThingCard;
import com.threerings.everything.rpc.EverythingService;
import com.threerings.everything.server.persist.CardRecord;
import com.threerings.everything.server.persist.GameRepository;
import com.threerings.everything.server.persist.GridRecord;
import com.threerings.everything.server.persist.LikeRecord;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.RecruitGiftRecord;
import com.threerings.everything.server.persist.ThingRepository;

import static com.threerings.everything.Log.log;

/**
 * Implements {@link EverythingService}.
 */
public class EverythingServlet extends EveryServiceServlet
    implements EverythingService
{
    public SessionData validateSession (OOOUser user, int tzOffset) throws ServiceException
    {
        SessionData data = new SessionData();
        for (News news : _playerLogic.resolveNames(_gameRepo.loadLatestNews())) {
            data.news = news;
        }
        data.powerups = Maps.newHashMap();
        data.everythingURL = _fbconf.getFacebookAppURL("http");
        data.backendURL = _app.getBackendURL();
        data.facebookAppId = _fbconf.getFacebookAppId();
        data.likes = Lists.newArrayList();
        data.dislikes = Lists.newArrayList();

        if (user == null) {
            log.info("Have no user, allowing guest", "tzOffset", tzOffset);
            data.name = PlayerName.createGuest();
            return data; // allow the player to do some things anonymously
        }

        // compute the player's timezone (note: tzOffset is minutes *before* GMT)
        String tz = String.format("GMT%+03d:%02d", -tzOffset/60, tzOffset%60);

        PlayerRecord player = _playerRepo.loadPlayer(user.userId);
        if (player == null) {
            Tuple<String, String> fbinfo = _userLogic.getExtAuthInfo(
                ExternalAuther.FACEBOOK, user.userId);
            if (fbinfo == null || fbinfo.right == null) {
                log.info("Have no session key for user, can't create player", "who", user.userId,
                         "fbinfo", fbinfo);
                data.name = PlayerName.createGuest();
                return data; // allow the player to do some things anonymously
            }

            // load up this player's Facebook profile info
            FacebookClient fbclient = new DefaultFacebookClient(fbinfo.right);
            long facebookId = Long.parseLong(fbinfo.left);
            User fbuser;
            try {
                fbuser = fbclient.fetchObject("me", User.class);
            } catch (FacebookException fbe) {
                log.warning("Failed to load Facebook profile info", "who", user.userId,
                            "fbinfo", fbinfo, fbe);
                throw new ServiceException(E_FACEBOOK_DOWN);
            }

            // create a new player with their Facebook data
            String bdstr = fbuser.getBirthday();
            long birthday = 0L;
            try {
                if (bdstr != null) {
                    Calendar cal = Calendar.getInstance();
                    if (bdstr.indexOf(",") == -1) {
                        cal.setTime(_bdfmt.parse(bdstr));
                    } else {
                        cal.setTime(_bfmt.parse(bdstr));
                    }
                    birthday = cal.getTimeInMillis();
                    log.info("Parsed Facebook birthday", "bday", bdstr, "date", cal.getTime());
                }
            } catch (Exception e) {
                log.info("Cannot parse Facebook birthday", "who", user.username, "bday", bdstr,
                         "err", e.getMessage());
            }
            player = _playerRepo.createPlayer(
                user.userId, facebookId, fbuser.getFirstName(), fbuser.getLastName(), birthday, tz);
            _gameRepo.startCollection(user.userId);
            _playerRepo.recordFeedItem(player.userId, FeedItem.Type.JOINED, 0, "");
            log.info("Hello newbie!", "who", player.who(), "surname", player.surname,
                     "tz", tz, "fbid", player.facebookId);

            try { // tell Samsara about the user's real name
                _userLogic.updateUser(
                    user.userId, null, null, player.name + " " + player.surname);
            } catch (Exception e) {
                log.warning("Failed to report real name to Samsara", "who", player.who(), e);
            }

            // transfer any escrowed cards into their collection
            _gameRepo.unescrowCards(fbinfo.left, player);

            // if they have a seed cookie, grant them a single card from their seed series
            String seed = CookieUtil.getCookieValue(
                getThreadLocalRequest(), AuthServlet.SEED_COOKIE);
            if (!StringUtil.isBlank(seed)) {
                try {
                    int thingId = _thingLogic.getThingIndex().pickSeedThing(
                        Integer.parseInt(seed));
                    if (thingId != 0) {
                        _gameRepo.createCard(player.userId, thingId, 0);
                        log.info("Granted seed thing to new player", "who", player.who(),
                                 "thing", thingId);
                    }
                } catch (Exception e) {
                    log.warning("Failed to process seed", "who", player.who(), "seed", seed, e);
                }
            }

            // look up their friends' facebook ids and make friend mappings for them
            updateFacebookInfo(player, fbinfo.right);

        } else {
            // if this is not their first session, update their last session timestamp
            long now = System.currentTimeMillis(), elapsed = now - player.lastSession.getTime();
            _playerRepo.recordSession(player, now, tz);
            log.info("Welcome back", "who", player.who(), "gone", elapsed);

            // check to see if they made FB friends with any existing Everything players (this also
            // updates other Facebook ephemera like first and last name)
            Tuple<String, String> fbinfo = _userLogic.getExtAuthInfo(
                ExternalAuther.FACEBOOK, user.userId);
            if (fbinfo != null) {
                updateFacebookInfo(player, fbinfo.right);
            }
        }

        data.name = player.getName();
        data.isEditor = player.isEditor;
        data.isAdmin = user.isAdmin();
        data.isMaintainer = user.holdsToken(OOOUser.MAINTAINER);
        data.coins = player.coins;
        data.powerups = _gameRepo.loadPowerups(player.userId);
        for (LikeRecord rec : _playerRepo.loadLikes(player.userId)) {
            (rec.like ? data.likes : data.dislikes).add(rec.categoryId);
        }
        GridRecord grid = _gameRepo.loadGrid(player.userId);
        if (grid != null) {
            data.gridsConsumed = grid.gridId;
            data.gridExpires = grid.expires.getTime();
        }
        return data;
    }

    // from interface EverythingService
    public SessionData validateSession (String version, int tzOffset) throws ServiceException
    {
        return validateSession(getUser(), tzOffset);
    }

    // from interface EverythingService
    public FeedResult getRecentFeed () throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        FeedResult result = new FeedResult();

        // load up their friends' recent activiteis
        List<FeedItem> items = _playerRepo.loadRecentFeed(player.userId, RECENT_FEED_ITEMS)
            .toList();
        aggregateFeed(player.userId, items);
        result.items = _playerLogic.resolveNames(items, player.getName());

        // if this player is an editor, load up recent comments on their series
        if (player.isEditor) {
            result.comments = Lists.newArrayList();
            long since = Calendars.now().zeroTime().addDays(-RECENT_COMMENT_DAYS).toTime();
            Set<Integer> catIds = Sets.newHashSet();
            for (CategoryComment comment : _thingRepo.loadCommentsSince(player.userId, since)) {
                if (comment.commentor.userId != player.userId) {
                    if (catIds.add(comment.categoryId)) { // only add one comment per-category
                        result.comments.add(comment);
                    }
                }
            }
            _playerLogic.resolveNames(result.comments, player.getName());
// TODO: resolve names, where do we put them?
//             Map<Integer, Category> cats = Maps.newHashMap();
//             for (Category cat : _thingRepo.loadCategories(catIds)) {
//                 cats.put(cat.categoryId, cat);
//             }
        }

        // load up their pending gifts
        result.gifts = Lists.newArrayList();
        for (CardRecord gift : _gameRepo.loadGifts(player.userId)) {
            ThingCard card = new ThingCard();
            card.thingId = gift.thingId;
            card.received = gift.received.getTime();
            result.gifts.add(card);
        }

        // resolve the user's recruitment gift
        result.recruitGifts = resolveRecruitGifts(player);

        return result;
    }

    // from interface EverythingService
    public List<FeedItem> getUserFeed (int userId) throws ServiceException
    {
        PlayerRecord caller = getPlayer(), target = _playerRepo.loadPlayer(userId);
        if (target == null) {
            throw new ServiceException(E_UNKNOWN_USER);
        }
        List<FeedItem> items = _playerRepo.loadUserFeed(target.userId, USER_FEED_ITEMS).toList();
        aggregateFeed(caller == null ? 0 : caller.userId, items);
        return _playerLogic.resolveNames(items, target.getName());
    }

    // from interface EverythingService
    public List<PlayerStats> getFriends () throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        Set<Integer> ids = _playerRepo.loadFriendIds(player.userId).toSet();
        ids.add(player.userId);
        Map<Integer, PlayerStats> stats = Maps.newHashMap();
        for (PlayerStats pstat : _gameRepo.loadCollectionStats(ids, _thingLogic.getThingIndex())) {
            stats.put(pstat.name.userId, pstat);
        }
        for (PlayerRecord prec : _playerRepo.loadPlayers(stats.keySet())) {
            PlayerStats pstat = stats.get(prec.userId);
            pstat.name = prec.getName();
            pstat.lastSession = new Date(prec.lastSession.getTime());
        }
        // friend records may exist for non-players (because a friend may arrive from facebook but
        // never register, but then next time around Everything will notice the fbid to samid
        // mapping and create a friend record for the friend), so we have to filter out players for
        // whom no name was found
        return Lists.newArrayList(Iterables.filter(stats.values(), new Predicate<PlayerStats>() {
            public boolean apply (PlayerStats stat) {
                return stat.name.name != null;
            }
        }));
    }

    // from interface EverythingService
    public CreditsResult getCredits () throws ServiceException
    {
        CreditsResult result = new CreditsResult();
        result.design = _playerRepo.loadPlayerName(2); // mdb
        result.art = _playerRepo.loadPlayerName(30); // josh
        result.code = Lists.newArrayList(
            _playerRepo.loadPlayerName(2), _playerRepo.loadPlayerName(25)); // mdb & ray
        final Multiset<Integer> edinfo = _thingRepo.loadEditorInfo();
        result.editors = Lists.newArrayList(
            _playerRepo.loadPlayerNames(edinfo.elementSet()).values());
        Collections.sort(result.editors, new Comparator<PlayerName>() {
            public int compare (PlayerName one, PlayerName two) {
                return Comparators.compare(edinfo.count(two.userId),
                                           edinfo.count(one.userId));
            }
        });
        return result;
    }

    // from interface EverythingService
    public void storyPosted (String tracking) throws ServiceException
    {
        // we used to report this for tracking, now we don't
    }

    protected void aggregateFeed (int callerId, List<FeedItem> items)
    {
        Map<ItemKey, FeedItem> imap = Maps.newHashMap();
        Calendar cal = Calendar.getInstance();
        for (Iterator<FeedItem> iter = items.iterator(); iter.hasNext(); ) {
            FeedItem item = iter.next();
            cal.setTime(item.when);
            int date = cal.get(Calendar.DAY_OF_YEAR);
            ItemKey key = new ItemKey(item, date);
            FeedItem oitem = imap.get(key);
            if (oitem  != null) {
                oitem.objects.addAll(item.objects);
                iter.remove();
            } else {
                imap.put(key, item);
            }
        }
    }

    protected void updateFacebookInfo (final PlayerRecord prec, String fbSessionKey)
    {
        final FacebookClient fbclient = new DefaultFacebookClient(fbSessionKey);
        _app.getExecutor().execute(new Runnable() {
            public void run () {
                try {
                    // ask Facebook for their list of friends
                    Connection<User> fbFriends = fbclient.fetchConnection(
                        "me/friends", User.class, Parameter.with("fields", "id"));
                    List<String> fbFriendIds = Lists.transform(
                        fbFriends.getData(), new Function<User,String>() {
                            public String apply (User user) {
                                return user.getId();
                            }
                        });
                    // map those friends to Samsara user ids
                    Set<Integer> allFriendIds = Sets.newHashSet(
                        _userLogic.mapExtAuthIds(ExternalAuther.FACEBOOK, fbFriendIds).values());
                    // retain only those that have Everything accounts
                    allFriendIds.retainAll(_playerRepo.loadPlayerNames(allFriendIds).keySet());
                    // load our current friends
                    Set<Integer> curFriendIds = _playerRepo.loadFriendIds(prec.userId).toSet();
                    // figure out new and old friends
                    Set<Integer> newFriendIds = Sets.difference(allFriendIds, curFriendIds);
                    Set<Integer> oldFriendIds = Sets.difference(curFriendIds, allFriendIds);
                    // add any newly-acquired friends
                    if (!newFriendIds.isEmpty()) {
                        log.info("Wiring up friends", "who", prec.who(), "friends", newFriendIds);
                        _playerRepo.addFriends(prec.userId, newFriendIds);
                    }
                    // remove any old friends
                    if (!oldFriendIds.isEmpty()) {
                        int removed = _playerRepo.removeFriends(prec.userId, oldFriendIds);
                        log.info("Removing friends",
                            "who", prec.who(), "friends", oldFriendIds, "removed", removed);
                    }
                } catch (Exception e) {
                    log.info("Failed to wire up Facebook friends", "who", prec.who(),
                             "error", e.getMessage());
                }

                // check to see if this player's first or last name has changed
                User fbuser;
                try {
                    fbuser = fbclient.fetchObject("me", User.class);
                    String name = StringUtil.getOr(fbuser.getFirstName(), prec.name);
                    String surname = StringUtil.getOr(fbuser.getLastName(), prec.surname);
                    if (!prec.name.equals(name) || !prec.surname.equals(surname)) {
                        log.info("Updating name", "who", prec.who(),
                                 "name", name, "surname", surname);
                        _playerRepo.updateName(prec.userId, name, surname);
                    }
                } catch (FacebookException fbe) {
                    log.warning("Failed to load Facebook profile info", "who", prec.who(), fbe);
                }
            }
        });
    }

    /**
     * Resolve the player's daily recruitment gift.
     */
    protected List<Card> resolveRecruitGifts (PlayerRecord player)
    {
        RecruitGiftRecord recruit = _playerRepo.loadRecruitGifts(player.userId);
        if (recruit == null || (recruit.expires.getTime() < System.currentTimeMillis())) {
            // only give gifts if they're not super new and if they actually used yesterday's
            // gifts (or didn't get any yesterday).
            boolean giveGifts = !player.isNewByDays(2) &&
                ((recruit == null) || !recruit.isUnused());
            int[] gifts;
            if (giveGifts) {
                gifts = Ints.toArray(_thingLogic.getThingIndex().pickRecruitmentThings(
                    _playerRepo.loadLikes(player.userId)));
            } else {
                gifts = new int[0];
            }
            recruit = _playerRepo.storeRecruitGifts(player, gifts);
        }
        List<Card> list = Lists.newArrayListWithCapacity(recruit.giftIds.length);
        for (int giftId : recruit.giftIds) {
            list.add((giftId == 0) ? null : _gameLogic.resolveCard(giftId));
        }
        return list;
    }

    protected static class ItemKey {
        public final int actorId;
        public final FeedItem.Type type;
        public final int targetId;
        public final int date;

        public ItemKey (FeedItem item, int date) {
            this.actorId = item.actor.userId;
            this.type = item.type;
            this.targetId = (item.target == null) ? 0 : item.target.userId;
            this.date = date;
        }

        public int hashCode () {
            return actorId ^ type.hashCode() ^ targetId ^ date;
        }

        public boolean equals (Object other) {
            ItemKey okey = (ItemKey)other;
            return actorId == okey.actorId && type == okey.type && targetId == okey.targetId &&
                date == okey.date;
        }
    }

    @Inject protected EverythingApp _app;
    @Inject protected FacebookConfig _fbconf;
    @Inject protected GameLogic _gameLogic;
    @Inject protected GameRepository _gameRepo;
    @Inject protected PlayerLogic _playerLogic;
    @Inject protected ThingLogic _thingLogic;
    @Inject protected ThingRepository _thingRepo;
    @Inject protected UserLogic _userLogic;

    /** Used to parse Facebook profile birthdays. */
    protected static SimpleDateFormat _bfmt = new SimpleDateFormat("MMMM dd, yyyy");

    /** Used to parse Facebook profile birthdays that lack years. */
    protected static SimpleDateFormat _bdfmt = new SimpleDateFormat("MMMM dd");

    /** The maximum number of recent feed items returned. */
    protected static final int RECENT_FEED_ITEMS = 150;

    /** The maximum number of user feed items returned. */
    protected static final int USER_FEED_ITEMS = 150;

    /** The number of days into the past we look for recent category comments. */
    protected static final int RECENT_COMMENT_DAYS = 3;
}
