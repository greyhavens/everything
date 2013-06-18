//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.server;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import com.google.inject.Inject;

import com.threerings.app.client.ServiceException;
import com.threerings.app.server.ServletAuthUtil;
import com.threerings.app.server.ServletLogic;
import com.threerings.user.ExternalAuther;
import com.threerings.user.OOOUser;

import static com.threerings.everything.Log.log;
import static com.threerings.everything.rpc.JSON.*;

public class JsonEverythingServlet extends JsonServiceServlet {

    protected Object handle (String method)
        throws IOException, ServiceException {
        if ("/validateSession".equals(method)) {
            ValidateSession args = readArgs(ValidateSession.class);
            HttpServletRequest req = threadLocalRequest();
            // if we have an auth token, load up the user for that session
            String authTok = _servletLogic.getAuthCookie(req);
            OOOUser user = (authTok == null) ? null : _servletLogic.getUser(authTok);
            // if the session was expired, or we never had one; start a new one
            if (user == null) {
                // TODO: validate Facebook credentials
                authTok = _servletLogic.externalLogon(
                    ExternalAuther.FACEBOOK, args.fbId, args.fbToken);
                log.info("Logging in", "fbId", args.fbId, "token", args.fbToken, "authTok", authTok);
                ServletAuthUtil.addAuthCookie(req, threadLocalResponse(), authTok, 180);
                user = _servletLogic.getUser(authTok);
            }
            log.info("Validating session", "authTok", authTok, "user", user);
            return _everyLogic.validateSession(req, user, args.tzOffset);

        } else if ("/getRecentFeed".equals(method)) {
            return _everyLogic.getRecentFeed(requirePlayer());

        } else if ("/getFriends".equals(method)) {
            return _everyLogic.getFriends(requirePlayer());

        } else if ("/getCredits".equals(method)) {
            return _everyLogic.getCredits();

        } else {
            return null;
        }
    }

    @Inject protected EverythingLogic _everyLogic;
    @Inject protected ServletLogic _servletLogic;
}
