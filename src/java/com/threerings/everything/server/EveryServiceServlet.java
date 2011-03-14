//
// $Id$

package com.threerings.everything.server;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.UnexpectedException;
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
    public String processCall (String payload)
        throws SerializationException
    {
        try {
            return super.processCall(payload);
        } finally {
            _perThreadPlayerRecord.remove();
        }
    }

    @Override // TODO: move this into AppServiceServlet
    protected void doUnexpectedFailure (Throwable error)
    {
        HttpServletRequest req = getThreadLocalRequest();
        HttpServletResponse rsp = getThreadLocalResponse();

        // if this is an "unexpected exception", unwrap the inner exception
        if (error instanceof UnexpectedException) {
            error = error.getCause();
        }

        // log the failure to the application log
        log.warning("Service request failure", "uri", req.getRequestURI(), error);

        // in case we failed while writing the response, we need to reset the response (from
        // RemoteServiceServlet)
        try {
            rsp.reset();
        } catch (IllegalStateException ex) {
            // If we can't reset the request, the only way to signal that something has gone wrong
            // is to throw an exception from here. It should be the case that we call the user's
            // implementation code before emitting data into the response, so the only time that
            // gets tripped is if the object serialization code blows up.
            throw new RuntimeException("Unable to report failure", error);
        }

        // send a standard failure response
        try {
            rsp.setContentType("text/plain");
            rsp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                rsp.getOutputStream().write(GENERIC_FAILURE_MSG.getBytes("UTF-8"));
            } catch (IllegalStateException ex) {
                // Handle the (unexpected) case where getWriter() was previously used
                rsp.getWriter().write(GENERIC_FAILURE_MSG);
            }
        } catch (IOException ioe) {
            log.warning("Failed to write failure response", ioe);
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
                return (user == null) ? null : _playerRepo.loadPlayer(user.userId);
            }
        };

    @Inject protected EverythingApp _app;
    @Inject protected PlayerRepository _playerRepo;

    // used by doUnexpectedFailure
    protected static final String GENERIC_FAILURE_MSG =
        "The call failed on the server; see server log for details";
}
