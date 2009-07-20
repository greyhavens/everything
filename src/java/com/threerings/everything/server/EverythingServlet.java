//
// $Id$

package com.threerings.everything.server;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJaxbRestClient;
import com.google.code.facebookapi.ProfileField;
import com.google.code.facebookapi.schema.User;
import com.google.code.facebookapi.schema.UsersGetInfoResponse;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;

import com.samskivert.util.CalendarUtil;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Tuple;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.UserLogic;

import com.threerings.everything.client.EverythingCodes;
import com.threerings.everything.client.EverythingService;
import com.threerings.everything.data.Build;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.CategoryComment;
import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.FriendStatus;
import com.threerings.everything.data.News;
import com.threerings.everything.data.SessionData;
import com.threerings.everything.server.persist.GameRepository;
import com.threerings.everything.server.persist.GridRecord;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.ThingRepository;

import static com.threerings.everything.Log.log;

/**
 * Implements {@link EverythingService}.
 */
public class EverythingServlet extends EveryServiceServlet
    implements EverythingService
{
    // from interface EverythingService
    public SessionData validateSession (String version, int tzOffset) throws ServiceException
    {
        if (!Build.version().equals(version)) {
            log.info("Rejecting stale client", "cversion", version, "sversion", Build.version());
            throw new ServiceException(EverythingCodes.E_STALE_APP);
        }

        OOOUser user = getUser();
        if (user == null) {
            throw new ServiceException(AppCodes.E_SESSION_EXPIRED);
        }

        PlayerRecord player = _playerRepo.loadPlayer(user.userId);
        if (player == null) {
            Tuple<String, String> fbinfo = _userLogic.getFacebookAuthInfo(user.userId);
            if (fbinfo == null || fbinfo.right == null) {
                log.info("Have no session key for user, can't create player", "who", user.userId,
                         "fbinfo", fbinfo);
                throw new ServiceException(AppCodes.E_SESSION_EXPIRED);
            }

            // load up this player's Facebook profile info
            FacebookJaxbRestClient fbclient = _faceLogic.getFacebookClient(fbinfo.right);
            long facebookId = Long.parseLong(fbinfo.left);
            Set<Long> ids = Collections.singleton(facebookId);
            EnumSet<ProfileField> fields = EnumSet.of(
                ProfileField.FIRST_NAME, ProfileField.LAST_NAME, ProfileField.BIRTHDAY);
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

            // compute the player's timezone (note: tzOffset is minutes *before* GMT)
            String tz = String.format("GMT%+03d:%02d", -tzOffset/60, tzOffset%60);

            // create a new player with their Facebook data
            User fbuser = uinfo.getUser().get(0);
            String bdstr = fbuser.getBirthday();
            long birthday = 0L;
            try {
                if (bdstr != null) {
                    birthday = _bfmt.parse(bdstr).getTime();
                }
            } catch (Exception e) {
                log.info("Cannot parse Facebook birthday", "who", user.username, "bday", bdstr);
            }
            player = _playerRepo.createPlayer(
                user.userId, facebookId, fbuser.getFirstName(), fbuser.getLastName(), birthday, tz);
            log.info("Hello newbie!", "who", player.who(), "name", player.who(), "tz", tz);

            // look up their friends' facebook ids and make friend mappings for them
            updateFacebookFriends(player, fbinfo.right);

        } else {
            // if this is not their first session, update their last session timestamp
            long now = System.currentTimeMillis(), elapsed = now - player.lastSession.getTime();
            _playerRepo.recordSession(player.userId, now);
            log.info("Welcome back", "who", player.who(), "gone", elapsed);

            // check to see if they made FB friends with any existing Everything players
            Tuple<String, String> fbinfo = _userLogic.getFacebookAuthInfo(user.userId);
            if (fbinfo != null) {
                updateFacebookFriends(player, fbinfo.right);
                // TEMP: if they have no facebook id stored, update it now
                if (player.facebookId == 0L) {
                    _playerRepo.updateFacebookId(player.userId, Long.parseLong(fbinfo.left));
                }
            }
        }

        SessionData data = new SessionData();
        data.name = player.getName();
        data.isEditor = player.isEditor;
        data.isAdmin = user.isAdmin();
        data.coins = player.coins;
        data.powerups = _gameRepo.loadPowerups(player.userId);
        GridRecord grid = _gameRepo.loadGrid(player.userId);
        if (grid != null) {
            data.gridsConsumed = grid.gridId;
            data.gridExpires = grid.expires.getTime();
        }
        for (News news : _playerLogic.resolveNames(_gameRepo.loadLatestNews())) {
            data.news = news;
        }
        data.facebookKey = _app.getFacebookKey();
        return data;
    }

    // from interface EverythingService
    public List<FeedItem> getRecentFeed () throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        List<FeedItem> items = Lists.newArrayList(
            _playerRepo.loadRecentFeed(player.userId, RECENT_FEED_ITEMS));
        Calendar cal = Calendar.getInstance();

        // if this player is an editor, load up recent comments on their feed
        if (player.isEditor) {
            // load up their recent comments
            cal.add(Calendar.DATE, -RECENT_COMMENT_DAYS);
            CalendarUtil.zeroTime(cal);
            Collection<CategoryComment> comments = _thingRepo.loadCommentsSince(
                player.userId, cal.getTimeInMillis());

            // load up the categories to which those comments apply
            Set<Integer> catIds = Sets.newHashSet();
            for (Iterator<CategoryComment> iter = comments.iterator(); iter.hasNext(); ) {
                CategoryComment comment = iter.next();
                if (comment.commentor.userId == player.userId) {
                    iter.remove(); // prune comments by us, we know we wrote them
                } else if (!catIds.add(comment.categoryId)) {
                    iter.remove(); // prune any comments after the first about the same category
                }
            }
            IntMap<Category> cats = IntMaps.newHashIntMap();
            for (Category cat : _thingRepo.loadCategories(catIds)) {
                cats.put(cat.categoryId, cat);
            }

            // create faux feed entries for each of these comments
            for (CategoryComment comment : comments) {
                FeedItem item = new FeedItem();
                item.actor = comment.commentor;
                item.when = comment.when;
                item.type = FeedItem.Type.COMMENT;
                item.target = player.getName();
                item.objects = Lists.newArrayList(cats.get(comment.categoryId).name);
                item.message = String.valueOf(comment.categoryId); // hax0rz!
                items.add(item);
            }
            Collections.sort(items);
        }

        // aggregate these results a bit
        Map<ItemKey, FeedItem> imap = Maps.newHashMap();
        for (Iterator<FeedItem> iter = items.iterator(); iter.hasNext(); ) {
            FeedItem item = iter.next();
            if (item.message != null) {
                // null out the message if we're not the target
                if (item.target != null && player.userId != item.target.userId) {
                    item.message = null;
                } else {
                    // if we are the target, don't try to aggregate this message
                    continue;
                }
            }
            ItemKey key = new ItemKey(item, cal);
            FeedItem oitem = imap.get(key);
            if (oitem  != null) {
                oitem.objects.addAll(item.objects);
                iter.remove();
            } else {
                imap.put(key, item);
            }
        }

        // finally resolve the names in all the records that remain
        return _playerLogic.resolveNames(items, player.getName());
    }

    // from interface EverythingService
    public List<FriendStatus> getFriends () throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        return Lists.newArrayList(_playerRepo.loadFriendStatus(player.userId));
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
                    Set<Integer> newFriendIds =
                        Sets.newHashSet(_userLogic.mapFacebookIds(fbFriendIds).values());
                    // remove all friends for whom we already have a mapping
                    newFriendIds.removeAll(_playerRepo.loadFriendIds(prec.userId));
                    // finally add mappings for any new friends we've discovered
                    if (newFriendIds.size() > 0) {
                        log.info("Wiring up friends", "who", prec.who(), "friends", newFriendIds);
                        _playerRepo.addFriends(prec.userId, newFriendIds);
                    }
                } catch (Exception e) {
                    log.info("Failed to wire up Facebook friends", "who", prec.who(),
                             "error", e.getMessage());
                }
            }
        });
    }

    protected static class ItemKey {
        public final int actorId;
        public final FeedItem.Type type;
        public final int targetId;
        public final int dayOfYear;

        public ItemKey (FeedItem item, Calendar cal) {
            this.actorId = item.actor.userId;
            this.type = item.type;
            cal.setTime(item.when);
            this.dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
            this.targetId = (item.target == null) ? 0 : item.target.userId;
        }

        public int hashCode () {
            return actorId ^ type.hashCode() ^ targetId ^ dayOfYear;
        }

        public boolean equals (Object other) {
            ItemKey okey = (ItemKey)other;
            return actorId == okey.actorId && type == okey.type && targetId == okey.targetId &&
                dayOfYear == okey.dayOfYear;
        }
    }

    @Inject protected EverythingApp _app;
    @Inject protected FacebookLogic _faceLogic;
    @Inject protected GameLogic _gameLogic;
    @Inject protected GameRepository _gameRepo;
    @Inject protected PlayerLogic _playerLogic;
    @Inject protected ThingRepository _thingRepo;
    @Inject protected UserLogic _userLogic;

    /** Used to parse Facebook profile birthdays. */
    protected static SimpleDateFormat _bfmt = new SimpleDateFormat("MMMM dd, yyyy");

    /** The maximum number of recent feed items returned. */
    protected static final int RECENT_FEED_ITEMS = 50;

    /** The number of days into the past we look for recent category comments. */
    protected static final int RECENT_COMMENT_DAYS = 3;
}
