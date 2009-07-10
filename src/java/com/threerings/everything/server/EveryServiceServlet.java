//
// $Id$

package com.threerings.everything.server;

import com.google.inject.Inject;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.AppServiceServlet;

import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;

/**
 * Extends {@link AppServiceServlet} with some additional useful bits.
 */
public abstract class EveryServiceServlet extends AppServiceServlet
{
    protected PlayerRecord requirePlayer ()
        throws ServiceException
    {
        return requirePlayer(requireUser());
    }

    protected PlayerRecord requirePlayer (OOOUser user)
        throws ServiceException
    {
        PlayerRecord player = _playerRepo.loadPlayer(user.userId);
        if (player == null) {
            throw new ServiceException(AppCodes.E_SESSION_EXPIRED);
        }
        return player;
    }

    protected PlayerRecord requireEditor ()
        throws ServiceException
    {
        return requireEditor(requireUser());
    }

    protected PlayerRecord requireEditor (OOOUser user)
        throws ServiceException
    {
        PlayerRecord record = requirePlayer(user);
        if (!record.isEditor && !user.isAdmin()) {
            throw new ServiceException(AppCodes.E_ACCESS_DENIED);
        }
        return record;
    }

    @Inject protected PlayerRepository _playerRepo;
}
