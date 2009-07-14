//
// $Id$

package com.threerings.everything.server;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJaxbRestClient;
import com.google.code.facebookapi.ProfileField;
import com.google.code.facebookapi.schema.User;
import com.google.code.facebookapi.schema.UsersGetInfoResponse;

import com.samskivert.util.Tuple;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.UserLogic;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.EverythingCodes;
import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.SessionData;
import com.threerings.everything.server.persist.GameRepository;
import com.threerings.everything.server.persist.GridRecord;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;

import static com.threerings.everything.Log.log;

/**
 * Implements {@link EverythingService}.
 */
public class EverythingServlet extends EveryServiceServlet
    implements EverythingService
{
    // from interface EverythingService
    public SessionData validateSession (int tzOffset) throws ServiceException
    {
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
            Set<Long> ids = Collections.singleton(Long.parseLong(fbinfo.left));
            EnumSet<ProfileField> fields = EnumSet.of(
                ProfileField.FIRST_NAME, ProfileField.LAST_NAME, ProfileField.BIRTHDAY);
            UsersGetInfoResponse uinfo;
            try {
                uinfo = (UsersGetInfoResponse)fbclient.users_getInfo(ids, fields);
            } catch (FacebookException fbe) {
                log.warning("Failed to load Facebook profile info", "who", user.userId,
                            "fbinfo", fbinfo, fbe);
                throw new ServiceException(EverythingCodes.E_FACEBOOK_DOWN);
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
                user.userId, fbuser.getFirstName(), fbuser.getLastName(), birthday, tz);
            log.info("Hello newbie!", "who", player.who(), "name", player.who(), "tz", tz);

            // look up their friends' facebook ids and make friend mappings for them
            try {
                List<String> friendIds = Lists.newArrayList();
                for (Long uid : fbclient.friends_get().getUid()) {
                    friendIds.add(uid.toString());
                }
                if (friendIds.size() > 0) {
                    log.info("Wiring up friends", "who", user.username, "friends", friendIds);
                    _playerRepo.addFriends(
                        user.userId, _userLogic.mapFacebookIds(friendIds).values());
                }
            } catch (Exception e) {
                log.info("Failed to look up Facebook friends", "who", user.username,
                         "error", e.getMessage());
            }

        } else {
            // if this is not their first session, update their last session and grant free flips
            // they've accumulated since their previous session
            long now = System.currentTimeMillis(), elapsed = now - player.lastSession.getTime();
            float extraFlips = _gameLogic.computeFreeFlipsEarned(player.freeFlips, elapsed);
            _playerRepo.recordSession(player.userId, now, extraFlips);
            log.info("Welcome back", "who", player.who(), "gone", elapsed, "flips", extraFlips);
        }

        SessionData data = new SessionData();
        data.name = player.getName();
        data.isEditor = player.isEditor;
        data.isAdmin = user.isAdmin();
        data.coins = player.coins;
        GridRecord grid = _gameRepo.loadGrid(player.userId);
        data.gridsConsumed = (grid == null) ? 0 : grid.gridId;
        return data;
    }

    // from interface EverythingService
    public List<FeedItem> getRecentFeed () throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        List<FeedItem> items = _playerRepo.loadRecentFeed(player.userId, RECENT_FEED_ITEMS);

        // aggregate these results a bit
        Calendar cal = Calendar.getInstance();
        Map<ItemKey, FeedItem> imap = Maps.newHashMap();
        for (Iterator<FeedItem> iter = items.iterator(); iter.hasNext(); ) {
            FeedItem item = iter.next();
            ItemKey key = new ItemKey(item, cal);
            FeedItem oitem = imap.get(key);
            if (oitem  != null) {
                oitem.objects.addAll(item.objects);
                iter.remove();
            } else {
                imap.put(key, item);
            }
        }

        return items;
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

    @Inject protected FacebookLogic _faceLogic;
    @Inject protected UserLogic _userLogic;
    @Inject protected GameLogic _gameLogic;
    @Inject protected GameRepository _gameRepo;

        /** Used to parse Facebook profile birthdays. */
    protected static SimpleDateFormat _bfmt = new SimpleDateFormat("MMMM dd, yyyy");

    /** The maximum number of recent feed items returned. */
    protected static final int RECENT_FEED_ITEMS = 50;
}
