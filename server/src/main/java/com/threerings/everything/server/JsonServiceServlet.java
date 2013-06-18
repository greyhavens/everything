//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.server;

import javax.servlet.http.HttpServletRequest;

import com.google.inject.Inject;

import com.threerings.app.client.ServiceException;
import com.threerings.app.data.AppCodes;
import com.threerings.app.server.ServletLogic;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;
import com.threerings.user.OOOUser;

import static com.threerings.everything.Log.log;

public abstract class JsonServiceServlet extends JsonServlet {

    protected OOOUser getUser (HttpServletRequest req) {
        return _servletLogic.getUser(req);
    }

    protected OOOUser requireUser (HttpServletRequest req) throws ServiceException {
        OOOUser user = getUser(req);
        ServiceException.require(user != null, AppCodes.E_SESSION_EXPIRED);
        return user;
    }

    protected PlayerRecord getPlayer (HttpServletRequest req) {
        return getPlayer(getUser(req));
    }

    protected PlayerRecord getPlayer (OOOUser user) {
        return (user == null) ? null : _playerRepo.loadPlayer(user.userId);
    }

    protected PlayerRecord requirePlayer (HttpServletRequest req) throws ServiceException {
        OOOUser user = getUser(req);
        PlayerRecord player = getPlayer(user);
        if (player == null) {
            log.warning("Missing player record for user in requirePlayer?",
                        "who", (user != null) ? user.userId : null);
            throw ServiceException.sessionExpired();
        }
        return player;
    }

    @Inject protected PlayerRepository _playerRepo;
    @Inject protected ServletLogic _servletLogic;
}
