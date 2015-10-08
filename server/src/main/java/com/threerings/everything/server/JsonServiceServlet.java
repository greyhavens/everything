//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.threerings.app.client.ServiceException;
import com.threerings.app.data.AppCodes;
import com.threerings.app.server.ServletLogic;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;
import com.threerings.user.OOOUser;

import static com.threerings.everything.Log.log;

public abstract class JsonServiceServlet extends JsonServlet {

    protected void doPost (HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        try {
            super.doPost(req, rsp);
        } finally {
            _perThreadUser.remove();
            _perThreadPlayer.remove();
        }
    }

    protected OOOUser getUser () {
        return _perThreadUser.get();
    }

    protected OOOUser requireUser () throws ServiceException {
        OOOUser user = getUser();
        ServiceException.require(user != null, AppCodes.E_SESSION_EXPIRED);
        return user;
    }

    protected PlayerRecord getPlayer () {
        return _perThreadPlayer.get();
    }

    protected PlayerRecord requirePlayer () throws ServiceException {
        PlayerRecord player = getPlayer();
        if (player == null) {
            OOOUser user = getUser();
            if (user != null) log.warning("No player for user in requirePlayer?",
                                          "who", user.userId);
            else log.info("No authed user in requirePlayer",
                          "authcook", _servletLogic.getAuthCookie(threadLocalRequest()));
            throw ServiceException.sessionExpired();
        }
        return player;
    }

    protected final ThreadLocal<OOOUser> _perThreadUser = new ThreadLocal<OOOUser>() {
        @Override protected OOOUser initialValue () {
            return _servletLogic.getUser(threadLocalRequest());
        }
    };

    protected final ThreadLocal<PlayerRecord> _perThreadPlayer = new ThreadLocal<PlayerRecord>() {
        @Override protected PlayerRecord initialValue () {
            OOOUser user = getUser();
            return (user == null) ? null : _playerRepo.loadPlayer(user.userId);
        }
    };

    @Inject protected PlayerRepository _playerRepo;
    @Inject protected ServletLogic _servletLogic;
}
