//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.StringUtil;

import com.threerings.app.client.ServiceException;
import com.threerings.app.data.AppCodes;
import com.threerings.app.server.ServletAuthUtil;
import com.threerings.facebook.servlet.FacebookAppServlet;
import com.threerings.user.OOOUser;
import com.threerings.user.depot.DepotUserRepository;

import static com.threerings.everything.Log.log;

/**
 * Authenticates a user and redirects them to the main page (with the appropriate app version) or
 * redirects them to the "You need to add the Everything app if you want to play" page if they
 * haven't added the app.
 */
public class AuthServlet extends FacebookAppServlet
{
    /** The name of the cookie in which we store the seed category. */
    public static final String SEED_COOKIE = "s";

    @Override // from HttpServlet
    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        // if we have auth info on our URL, use that (for local testing)
        String username = ParameterUtil.getParameter(req, "user", true);
        if (username != null && _app.isLocalTest()) {
            try {
                OOOUser user = _userRepo.loadUser(username, false);
                ServiceException.require(user != null, AppCodes.E_NO_SUCH_USER);
                String authtok = _userRepo.registerSession(user, 3);
                ServletAuthUtil.addAuthCookie(req, rsp, authtok, 3);
                rsp.sendRedirect("index.html");
                return;
            } catch (ServiceException se) {
                log.warning("Failed to logon", "user", username, "se", se);
            }
        }

        // if we have a seed category, stuff that into a session cookie so we can pick it up again
        // after the client adds the app and validates their session for the first time
        String seed = ParameterUtil.getParameter(req, "seed", false);
        if (!StringUtil.isBlank(seed)) {
            Cookie c = new Cookie(SEED_COOKIE, seed);
            c.setPath("/");
            rsp.addCookie(c);
        }

        // we need to supply a full URL to doFacebookAuth so that it can redirect properly to https
        // or http even when the current request is http representing proxied https
        String indexURL = ServletAuthUtil.createURL(req, "/");

        // pass the buck to the app servlet, it may have to get jiggy
        doFacebookAuth(req, rsp, indexURL, true, false);
    }

    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        // Facebook now sends a POST request when loading our app iframe, but we'll still support
        // GET just in case the winds blow back in that direction
        doPost(req, rsp);
    }

    @Inject protected EverythingApp _app;
    @Inject protected DepotUserRepository _userRepo;

    protected static final String GWT_DEVPARAM = "gwt.codesvr";
}
