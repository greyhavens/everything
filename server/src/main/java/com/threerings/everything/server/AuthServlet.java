//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server;

import java.io.IOException;
import java.util.Random;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.StringUtil;

import com.threerings.app.client.ServiceException;
import com.threerings.app.server.ServletAuthUtil;
import com.threerings.facebook.servlet.FacebookAppServlet;

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
        // if we have auth info on our URL, use that (for gshell)
        String username = ParameterUtil.getParameter(req, "user", true);
        String passwd = ParameterUtil.getParameter(req, "pass", true);
        if (username != null && passwd != null) {
            try {
                _servletLogic.logon(req, rsp, username, StringUtil.md5hex(passwd), 2);
                // preserve a special argument that GWT needs to make devmode work
                String gwtbits = ParameterUtil.getParameter(req, GWT_DEVPARAM, false);
                if (gwtbits.length() > 0) {
                    gwtbits = "?" + GWT_DEVPARAM + "=" + gwtbits;
                }
                rsp.sendRedirect("index.html" + gwtbits);
                return;
            } catch (ServiceException se) {
                log.warning("Failed to logon", "user", username, "pass", passwd, "se", se);
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

    protected Random _rando = new Random();

    protected static final String GWT_DEVPARAM = "gwt.codesvr";
}
