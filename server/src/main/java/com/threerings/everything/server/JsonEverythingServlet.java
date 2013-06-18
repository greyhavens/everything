//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.server;

import java.io.IOException;

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
            String authTok = _servletLogic.getAuthCookie(threadLocalRequest());
            if (authTok == null) {
                // TODO: validate Facebook credentials
                authTok = _servletLogic.externalLogon(
                    ExternalAuther.FACEBOOK, args.fbId, args.fbToken);
                log.info("Logging in", "fbId", args.fbId, "token", args.fbToken, "authTok", authTok);
                ServletAuthUtil.addAuthCookie(
                    threadLocalRequest(), threadLocalResponse(), authTok, 180);
            }
            OOOUser user = _servletLogic.getUser(authTok);
            log.info("Validating session", "authTok", authTok, "user", user);
            return _everyLogic.validateSession(threadLocalRequest(), user, args.tzOffset);

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
