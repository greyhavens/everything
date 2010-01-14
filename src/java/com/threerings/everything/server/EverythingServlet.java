//
// $Id$

package com.threerings.everything.server;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJaxbRestClient;
import com.google.code.facebookapi.ProfileField;
import com.google.code.facebookapi.schema.User;
import com.google.code.facebookapi.schema.UsersGetInfoResponse;

import com.samskivert.servlet.util.CookieUtil;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Calendars;
import com.samskivert.util.Comparators;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.IntSets;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.common.UserLogic;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.client.Kontagent;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.CategoryComment;
import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.News;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.PlayerStats;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.SessionData;
import com.threerings.everything.data.ThingCard;
import com.threerings.everything.util.GameUtil;
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
    // from interface EverythingService
    public SessionData validateSession (String version, int tzOffset, String kontagentToken)
        throws ServiceException
    {
        SessionData data = new SessionData();
        data.candidate = _candidate;
        for (News news : _playerLogic.resolveNames(_gameRepo.loadLatestNews())) {
            data.news = news;
        }
        data.powerups = Maps.newHashMap();
        data.everythingURL = _app.getFacebookAppURL();
        data.kontagentHello = _app.getKontagentURL(Kontagent.PAGE_REQUEST);

        OOOUser user = getUser();
        if (user == null) {
            log.info("Have no user, allowing guest", "version", version, "tzOffset", tzOffset,
                     "tracking", kontagentToken);
            data.name = PlayerName.createGuest();
            return data; // allow the player to do some things anonymously
        }

        // compute the player's timezone (note: tzOffset is minutes *before* GMT)
        String tz = String.format("GMT%+03d:%02d", -tzOffset/60, tzOffset%60);

        PlayerRecord player = _playerRepo.loadPlayer(user.userId);
        if (player == null) {
            Tuple<String, String> fbinfo = _userLogic.getFacebookAuthInfo(user.userId);
            if (fbinfo == null || fbinfo.right == null) {
                log.info("Have no session key for user, can't create player", "who", user.userId,
                         "fbinfo", fbinfo);
                data.name = PlayerName.createGuest();
                return data; // allow the player to do some things anonymously
            }

            // load up this player's Facebook profile info
            FacebookJaxbRestClient fbclient = _faceLogic.getFacebookClient(fbinfo.right);
            long facebookId = Long.parseLong(fbinfo.left);
            Set<Long> ids = Collections.singleton(facebookId);
            EnumSet<ProfileField> fields = EnumSet.of(
                ProfileField.FIRST_NAME, ProfileField.LAST_NAME, ProfileField.BIRTHDAY,
                ProfileField.SEX, ProfileField.CURRENT_LOCATION);
            UsersGetInfoResponse uinfo;
            try {
                uinfo = (UsersGetInfoResponse)fbclient.users_getInfo(ids, fields);
            } catch (FacebookException fbe) {
                log.warning("Failed to load Facebook profile info", "who", user.userId,
                            "fbinfo", fbinfo, fbe);
                throw new ServiceException(E_FACEBOOK_DOWN);
            }
            if (uinfo.getUser().size() == 0) {
                log.warning("User has no Facebook profile info?", "who", user.userId,
                            "fbinfo", fbinfo);
                throw new ServiceException(AppCodes.E_SESSION_EXPIRED);
            }

            // create a new player with their Facebook data
            User fbuser = uinfo.getUser().get(0);
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
                     "tz", tz, "fbid", player.facebookId, "tracking", kontagentToken);

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
            updateFacebookFriends(player, fbinfo.right);

            // note that a new user added our app
            try {
                _kontLogic.reportNewUser(player, fbuser, kontagentToken);
            } catch (Exception e) {
                log.warning("Failed to report new user to Kontagent", "who", player.who(), e);
            }

        } else {
            // if this is not their first session, update their last session timestamp
            long now = System.currentTimeMillis(), elapsed = now - player.lastSession.getTime();
            _playerRepo.recordSession(player, now, tz);
            log.info("Welcome back", "who", player.who(), "gone", elapsed,
                     "tracking", kontagentToken);

            // check to see if they made FB friends with any existing Everything players
            Tuple<String, String> fbinfo = _userLogic.getFacebookAuthInfo(user.userId);
            if (fbinfo != null) {
                updateFacebookFriends(player, fbinfo.right);
            }
        }

        data.name = player.getName();
        data.isEditor = player.isEditor;
        data.isAdmin = user.isAdmin();
        data.isMaintainer = user.holdsToken(OOOUser.MAINTAINER);
        data.coins = player.coins;
        data.powerups = _gameRepo.loadPowerups(player.userId);
        data.likes = Lists.newArrayList();
        data.dislikes = Lists.newArrayList();
        for (LikeRecord rec : _playerRepo.loadLikes(player.userId)) {
            (rec.like ? data.likes : data.dislikes).add(rec.categoryId);
        }
        data.kontagentHello = _app.getKontagentURL(Kontagent.PAGE_REQUEST, "s", player.facebookId);
        GridRecord grid = _gameRepo.loadGrid(player.userId);
        if (grid != null) {
            data.gridsConsumed = grid.gridId;
            data.gridExpires = grid.expires.getTime();
        }
        return data;
    }

    // from interface EverythingService
    public FeedResult getRecentFeed () throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        FeedResult result = new FeedResult();

        // load up their friends' recent activiteis
        List<FeedItem> items = Lists.newArrayList(
            _playerRepo.loadRecentFeed(player.userId, RECENT_FEED_ITEMS));
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
//             IntMap<Category> cats = IntMaps.newHashIntMap();
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
        List<FeedItem> items = Lists.newArrayList(
            _playerRepo.loadUserFeed(target.userId, USER_FEED_ITEMS));
        aggregateFeed(caller == null ? 0 : caller.userId, items);
        return _playerLogic.resolveNames(items, target.getName());
    }

    // from interface EverythingService
    public List<PlayerStats> getFriends () throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        Set<Integer> ids = Sets.newHashSet(_playerRepo.loadFriendIds(player.userId));
        ids.add(player.userId);
        IntMap<PlayerStats> stats = IntMaps.newHashIntMap();
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
        final IntIntMap edinfo = _thingRepo.loadEditorInfo();
        result.editors = Lists.newArrayList(_playerRepo.loadPlayerNames(edinfo.keySet()).values());
        Collections.sort(result.editors, new Comparator<PlayerName>() {
            public int compare (PlayerName one, PlayerName two) {
                return Comparators.compare(edinfo.getOrElse(two.userId, 0),
                                           edinfo.getOrElse(one.userId, 0));
            }
        });
        return result;
    }

    // from interface EverythingService
    public void storyPosted (String tracking) throws ServiceException
    {
        _kontLogic.reportAction(
            Kontagent.POST, "s", requirePlayer().facebookId, "tu", "stream", "u", tracking);
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

    protected void updateFacebookFriends (final PlayerRecord prec, String fbSessionKey)
    {
        final FacebookJaxbRestClient fbclient = _faceLogic.getFacebookClient(fbSessionKey);
        _app.getExecutor().execute(new Runnable() {
            public void run () {
                try {
                    // ask Facebook for their list of friends
                    List<String> fbFriendIds = Lists.newArrayList();
                    for (Long uid : fbclient.friends_get().getUid()) {
                        fbFriendIds.add(uid.toString());
                    }
                    // map those friends to Samsara user ids
                    IntSet allFriendIds =
                        IntSets.create(_userLogic.mapFacebookIds(fbFriendIds).values());
                    // retain only those that have Everything accounts
                    allFriendIds.retainAll(_playerRepo.loadPlayerNames(allFriendIds).intKeySet());
                    // load our current friends
                    IntSet curFriendIds = IntSets.create(_playerRepo.loadFriendIds(prec.userId));
                    // figure out new and old friends
                    IntSet newFriendIds = IntSets.difference(allFriendIds, curFriendIds);
                    IntSet oldFriendIds = IntSets.difference(curFriendIds, allFriendIds);
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

                // TEMP: update birthdays for people who started when there was a bug
                if (prec.birthdate == 0) {
                    Set<Long> ids = Collections.singleton(prec.facebookId);
                    EnumSet<ProfileField> fields = EnumSet.of(ProfileField.BIRTHDAY);
                    UsersGetInfoResponse uinfo;
                    try {
                        uinfo = (UsersGetInfoResponse)fbclient.users_getInfo(ids, fields);
                    } catch (FacebookException fbe) {
                        log.warning("Failed to load Facebook profile info", "who", prec.who(), fbe);
                        return;
                    }
                    if (uinfo.getUser().size() == 0) {
                        log.warning("User has no Facebook profile info?", "who", prec.who());
                        return;
                    }

                    User fbuser = uinfo.getUser().get(0);
                    String bdstr = fbuser.getBirthday();
                    try {
                        if (bdstr != null) {
                            Date born = (bdstr.indexOf(",") == -1) ?
                                _bdfmt.parse(bdstr) : _bfmt.parse(bdstr);
                            log.info("Parsed birthday", "bday", bdstr, "date", born);
                            _playerRepo.updateBirthday(prec.userId, born.getTime());
                        }
                    } catch (Exception e) {
                        log.info("Cannot parse birthday", "who", prec.who(), "bday", bdstr,
                                 "err", e.getMessage());
                    }
                }
                // END TEMP
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
                // load all the player's things of rarity II or less.
                gifts = _thingLogic.getThingIndex().pickRecruitmentThings(
                    _thingRepo.loadPlayerThings(player.userId, null, Rarity.II)).toIntArray();
                ArrayUtil.shuffle(gifts);
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

    @Inject protected @Named(AppCodes.APPCANDIDATE) boolean _candidate;
    @Inject protected EverythingApp _app;
    @Inject protected FacebookLogic _faceLogic;
    @Inject protected GameLogic _gameLogic;
    @Inject protected GameRepository _gameRepo;
    @Inject protected KontagentLogic _kontLogic;
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
