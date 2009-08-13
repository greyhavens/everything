//
// $Id$

package com.threerings.everything.server;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.inject.Inject;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.AppServiceServlet;

import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;

import static com.threerings.everything.Log.log;

/**
 * Extends {@link AppServiceServlet} with some additional useful bits.
 */
public abstract class EveryServiceServlet extends AppServiceServlet
{
    @Override // from RemoteServiceServlet
    public String processCall (String payload) throws SerializationException
    {
        return super.processCall(payload);
    }

    protected PlayerRecord getPlayer ()
    {
        OOOUser user = getUser();
        return (user == null) ? null : _playerRepo.loadPlayer(user.userId);
    }

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
            log.warning("Missing player record for user in requirePlayer?", "who", user.userId);
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

    @Inject protected EverythingApp _app;
    @Inject protected PlayerRepository _playerRepo;
}
