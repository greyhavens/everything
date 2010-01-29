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
    public static PlayerRecord getLoadedPlayer ()
    {
        return _loadedPlayer.get();
    }

    @Override // from RemoteServiceServlet
    public String processCall (String payload)
        throws SerializationException
    {
        try {
            return super.processCall(payload);

        } finally {
            _perThreadPlayerRecord.remove();
            _loadedPlayer.remove();
        }
    }

    protected PlayerRecord getPlayer ()
    {
        return _perThreadPlayerRecord.get();
    }

    protected PlayerRecord requirePlayer ()
        throws ServiceException
    {
        PlayerRecord player = getPlayer();
        if (player == null) {
            OOOUser user = getUser();
            log.warning("Missing player record for user in requirePlayer?",
                "who", (user != null) ? user.userId : null);
            throw new ServiceException(AppCodes.E_SESSION_EXPIRED);
        }
        return player;
    }

    protected PlayerRecord requireEditor ()
        throws ServiceException
    {
        PlayerRecord record = requirePlayer();
        if (!record.isEditor && !getUser().isAdmin()) {
            throw new ServiceException(AppCodes.E_ACCESS_DENIED);
        }
        return record;
    }

    /** Provides the PlayerRecord corresponding to the current request. */
    protected transient ThreadLocal<PlayerRecord> _perThreadPlayerRecord =
        new ThreadLocal<PlayerRecord>() {
            @Override protected PlayerRecord initialValue ()
            {
                OOOUser user = getUser();
                PlayerRecord record = (user == null) ? null : _playerRepo.loadPlayer(user.userId);
                _loadedPlayer.set(record);
                return record;
            }
        };

    /** A static copy of the PlayerRecord, accessible anywhere. */
    protected static transient ThreadLocal<PlayerRecord> _loadedPlayer =
        new ThreadLocal<PlayerRecord>();

    @Inject protected EverythingApp _app;
    @Inject protected PlayerRepository _playerRepo;
}
