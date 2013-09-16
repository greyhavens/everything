//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.server;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import com.google.inject.Inject;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import com.restfb.types.User;

import com.threerings.app.client.ServiceException;
import com.threerings.app.server.ServletAuthUtil;
import com.threerings.app.server.ServletLogic;
import com.threerings.user.ExternalAuther;
import com.threerings.user.OOOUser;
import com.threerings.user.depot.ExternalAuthRepository;

import com.threerings.everything.rpc.EverythingService;
import static com.threerings.everything.Log.log;
import static com.threerings.everything.rpc.JSON.*;

public class JsonEverythingServlet extends JsonServiceServlet {

    protected Object handle (String method)
        throws IOException, ServiceException {
        if ("/validateSession".equals(method)) {
            ValidateSession args = readArgs(ValidateSession.class);
            HttpServletRequest req = threadLocalRequest();

            // TODO: if we have a valid authTok, do the FB session refresh in the background and
            // tell the client that everything is AOK a bit faster

            // ask Facebook who the owner of this session token is
            String fbId = getFacebookId(args.fbToken);
            // if we have an auth token, load up the user for that session
            String authTok = _servletLogic.getAuthCookie(req);
            OOOUser user = (authTok == null) ? null : _servletLogic.getUser(authTok);
            // if the session was expired, or we never had one; start a new one
            if (user == null) {
                authTok = _servletLogic.externalLogon(ExternalAuther.FACEBOOK, fbId, args.fbToken);
                log.info("Logging in", "fbId", fbId, "token", args.fbToken, "authTok", authTok);
                ServletAuthUtil.addAuthCookie(req, threadLocalResponse(), authTok, 180);
                user = _servletLogic.getUser(authTok);
            } else {
                // otherwise just make a note of their (possibly new) FB session token
                _extAuthRepo.updateExternalSession(ExternalAuther.FACEBOOK, fbId, args.fbToken);
            }
            log.info("Validating session", "authTok", authTok, "user", user);
            return _everyLogic.validateSession(req, user, args.tzOffset);

        } else if ("/getRecentFeed".equals(method)) {
            return _everyLogic.getRecentFeed(requirePlayer());

        } else if ("/getFriends".equals(method)) {
            return _everyLogic.getFriends(requirePlayer());

        } else if ("/getCredits".equals(method)) {
            return _everyLogic.getCredits();

        } else if ("/redeemPurchase".equals(method)) {
            RedeemPurchase args = readArgs(RedeemPurchase.class);
            return _purchLogic.redeemPurchase(requirePlayer(), args.sku, args.platform,
                                              args.token, args.receipt);

        } else {
            return null;
        }
    }

    protected String getFacebookId (String fbToken) throws ServiceException {
        // if we're testing (on the candidate server), return the id they asked for
        if (_app.isCandidate() && fbToken.startsWith("test:")) return fbToken.substring(5);
        // load up the Facebook user associated with this FB session token
        FacebookClient fbclient = new DefaultFacebookClient(fbToken);
        try {
            User user = fbclient.fetchObject("me", User.class, Parameter.with("fields", "id, name"));
            return user.getId();
        } catch (FacebookException fbe) {
            log.warning("Failed to resolve Facebook user", "fbToken", fbToken, fbe);
            throw new ServiceException(EverythingService.E_FACEBOOK_DOWN);
        }
    }

    @Inject protected EverythingApp _app;
    @Inject protected EverythingLogic _everyLogic;
    @Inject protected PurchaseLogic _purchLogic;
    @Inject protected ServletLogic _servletLogic;
    @Inject protected ExternalAuthRepository _extAuthRepo;
}
