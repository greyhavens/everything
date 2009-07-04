//
// $Id$

package com.threerings.everything.server;

import com.google.inject.Inject;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.server.AppServiceServlet;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.SessionData;
import com.threerings.everything.server.persist.EverythingRepository;
import com.threerings.everything.server.persist.PlayerRecord;

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
            return null;
        }

        PlayerRecord player = _everyRepo.loadPlayer(user.userId);
        if (player == null) {
            log.info("Have user but no player", "user", user);
            // create a new player record by talking to Facebook
            return null;
        }

        SessionData data = new SessionData();
        data.name = player.toName();
        data.isAdmin = user.isAdmin();
        return data;
    }

    // from interface EverythingService
    public Card getCard (int ownerId, int thingId) throws ServiceException
    {
        return null; // TODO
    }

    @Inject protected EverythingRepository _everyRepo;
}
