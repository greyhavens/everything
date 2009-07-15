//
// $Id$

package com.threerings.everything.server;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.inject.Inject;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.AppServiceServlet;

import com.threerings.everything.client.EverythingCodes;
import com.threerings.everything.data.Build;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;

/**
 * Extends {@link AppServiceServlet} with some additional useful bits.
 */
public abstract class EveryServiceServlet extends AppServiceServlet
{
    @Override // from RemoteServiceServlet
    public String processCall (String payload) throws SerializationException
    {
        String reqvers = CookieUtil.getCookieValue(
            getThreadLocalRequest(), EverythingCodes.VERS_COOKIE);
        if (!Build.VERSION.equals(reqvers)) {
            return RPC.encodeResponseForFailure(
                null, new IncompatibleRemoteServiceException(EverythingCodes.E_STALE_APP, null));
        }
        return super.processCall(payload);
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
