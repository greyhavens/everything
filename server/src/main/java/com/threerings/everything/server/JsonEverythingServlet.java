//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.server;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.threerings.app.client.ServiceException;
import com.threerings.app.server.ServletAuthUtil;
import com.threerings.app.server.ServletLogic;
import com.threerings.user.ExternalAuther;
import com.threerings.user.OOOUser;

import static com.threerings.everything.Log.log;

public class JsonEverythingServlet extends JsonServlet {

    protected Object handle (String method, HttpServletRequest req, HttpServletResponse rsp)
        throws IOException, ServiceException {
        if ("/validateSession".equals(method)) {
            ValidateSession args = readArgs(req, ValidateSession.class);
            String authTok = _servletLogic.getAuthCookie(req);
            if (authTok == null) {
                // TODO: validate Facebook credentials
                authTok = _servletLogic.externalLogon(
                    ExternalAuther.FACEBOOK, args.fbId, args.fbToken);
                log.info("Logging in", "fbId", args.fbId, "token", args.fbToken, "authTok", authTok);
                ServletAuthUtil.addAuthCookie(req, rsp, authTok, 180);
            }
            OOOUser user = _servletLogic.getUser(authTok);
            log.info("Validating session", "authTok", authTok, "user", user);
            return _everySvc.validateSession(user, args.tzOffset);

        } else if ("/getRecentFeed".equals(method)) {
            return _everySvc.getRecentFeed();

        } else if ("/getFriends".equals(method)) {
            return _everySvc.getFriends();

        } else if ("/getCredits".equals(method)) {
            return _everySvc.getCredits();

        } else {
            return null;
        }
    }

    protected static class ValidateSession {
        public String fbId;
        public String fbToken;
        public int tzOffset;
    }
    protected static class GetUserFeed {
        public int userId;
    }

    @Inject protected EverythingServlet _everySvc;
    @Inject protected ServletLogic _servletLogic;
}
