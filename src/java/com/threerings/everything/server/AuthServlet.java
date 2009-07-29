//
// $Id$

package com.threerings.everything.server;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.StringUtil;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;
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

        // if we're the release candidate, use /candidate/everything/ otherwise use the versionless
        // URL and let GWT complain if someone makes an out of date service request (mostly things
        // are fine from version to version and not using the versioned URL means we don't start
        // 404ing after a release)
        String indexPath = "/everything/";
        if (_appvers.equals(AppCodes.RELEASE_CANDIDATE)) {
            indexPath = "/" + AppCodes.RELEASE_CANDIDATE + indexPath;
        }

        // otherwise pass the buck to the app servlet, it may have to get jiggy
        doFacebookAuth(req, rsp, _app.getFacebookAppURL(), null, indexPath);
    }

    @Inject protected EverythingApp _app;
    @Inject protected @Named(AppCodes.APPVERS) String _appvers;
}
