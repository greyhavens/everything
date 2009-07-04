//
// $Id$

package com.threerings.everything.server;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.servlet.util.ParameterUtil;
import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.server.AppServlet;

import static com.threerings.everything.Log.log;

/**
 * Authenticates a user and redirects them to the main page (with the appropriate app version) or
 * redirects them to the "You need to add the Everything app if you want to play" page if they
 * haven't added the app.
 */
public class AuthServlet extends AppServlet
{
    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        try {
            // if they have no session key, they haven't added the app, if they have a session key
            // they have so we'll create an OOOUser record for them and send them to index.html
            if (!ParameterUtil.isSet(req, "fb_sig_session_key") || getUser(req, rsp) == null) {
                rsp.sendRedirect("addme.html");
            } else {
                rsp.sendRedirect("index.html");
            }
        } catch (ServiceException se) {
            log.warning("Failed to auth user", "se", se);
            rsp.sendRedirect("addme.html");
        }
    }
}
