//
// $Id$

package com.threerings.everything.server;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.google.inject.Inject;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJaxbRestClient;
import com.google.code.facebookapi.ProfileField;
import com.google.code.facebookapi.schema.User;
import com.google.code.facebookapi.schema.UsersGetInfoResponse;

import com.samskivert.util.Tuple;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.AppServiceServlet;
import com.threerings.samsara.app.server.UserLogic;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.EverythingCodes;
import com.threerings.everything.data.SessionData;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;

import static com.threerings.everything.Log.log;

/**
 * Implements {@link EverythingService}.
 */
public class EverythingServlet extends AppServiceServlet
    implements EverythingService
{
    // from interface EverythingService
    public SessionData validateSession () throws ServiceException
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
                ProfileField.FIRST_NAME, ProfileField.BIRTHDAY);
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
            player = _playerRepo.createPlayer(user.userId, fbuser.getFirstName(), birthday);
        }

        // if this is not their first session, update their last session and grant free flips
        // they've accumulated since their previous session
        if (!player.joined.equals(player.lastSession)) {
            long now = System.currentTimeMillis(), elapsed = now - player.lastSession.getTime();
            float extraFlips = _gameLogic.computeFreeFlipsEarned(player.freeFlips, elapsed);
            _playerRepo.recordSession(player.userId, now, extraFlips);
            log.info("Welcome back", "who", user.username, "gone", elapsed, "flips", extraFlips);
        }

        SessionData data = new SessionData();
        data.name = player.toName();
        data.isAdmin = user.isAdmin();
        return data;
    }

    @Inject protected PlayerRepository _playerRepo;
    @Inject protected FacebookLogic _faceLogic;
    @Inject protected UserLogic _userLogic;
    @Inject protected GameLogic _gameLogic;

    /** Used to parse Facebook profile birthdays. */
    protected static SimpleDateFormat _bfmt = new SimpleDateFormat("MMMM dd, yyyy");
}
