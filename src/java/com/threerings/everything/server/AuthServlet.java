//
// $Id$

package com.threerings.everything.server;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.StringUtil;
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
        // if we have auth info on our URL, use that (for gshell)
        String username = ParameterUtil.getParameter(req, "user", true);
        String passwd = ParameterUtil.getParameter(req, "pass", true);
        if (username != null && passwd != null) {
            try {
                _servletLogic.logon(username, StringUtil.md5hex(passwd), 2, rsp);
                rsp.sendRedirect("index.html");
                return;
            } catch (ServiceException se) {
                log.warning("Failed to logon", "user", username, "pass", passwd, "se", se);
            }
        }

        try {
            // if they have no session key, they haven't added the app, if they have a session key
            // they have so we'll create an OOOUser record for them and send them to index.html
            if (!ParameterUtil.isSet(req, "fb_sig_session_key") || getUser(req, rsp) == null) {
                // we're in an iframe so we have to send down some JavaScript that jimmies
                PrintWriter out = new PrintWriter(new OutputStreamWriter(rsp.getOutputStream()));
                out.println("<html><head><script language=\"JavaScript\">");
                out.println("window.top.location = 'http://www.facebook.com/login.php?api_key=" +
                            _app.getFacebookKey() + "&canvas=1&v=1.0';");
                out.println("</script></head></html>");
                out.close();
            } else {
                rsp.sendRedirect("index.html");
            }
        } catch (ServiceException se) {
            log.warning("Failed to auth user", "se", se);
            rsp.sendRedirect("addme.html");
        }
    }

    @Inject protected EverythingApp _app;
}
