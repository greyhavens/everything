//
// $Id$

package com.threerings.everything.server;

import java.io.IOException;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.StringUtil;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.AppServlet;

import com.threerings.everything.client.Kontagent;

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
                _servletLogic.logon(_app, req, rsp, username, StringUtil.md5hex(passwd), 2);
                rsp.sendRedirect("index.html");
                return;
            } catch (ServiceException se) {
                log.warning("Failed to logon", "user", username, "pass", passwd, "se", se);
            }
        }

        // we should get some sort of kontagent tracking data with this request
        Kontagent type = Kontagent.fromCode(req.getParameter("kc"));
        String vector = ParameterUtil.getParameter(req, "vec", "organic");
        String tracking = req.getParameter("t");
        switch (type) {
        case NOOP: break; // nothing!
        case INVITE:
            reportResponse(req, Kontagent.INVITE_RESPONSE, tracking);
            break;
        case NOTIFICATION:
            reportResponse(req, Kontagent.NOTIFICATION_RESPONSE, tracking);
            break;
        case NOTIFICATION_EMAIL:
            reportResponse(req, Kontagent.NOTIFICATION_EMAIL_RESPONSE, tracking);
            break;
        case OTHER_RESPONSE:
            // we need to generate a short tracking code for this user
            tracking = Integer.toHexString(_rando.nextInt());
            tracking = StringUtil.fill('0', 8-tracking.length()) + tracking;
            reportLanding(req, Kontagent.OTHER_RESPONSE, vector, tracking);
            break;
        default:
            log.warning("Got weird Kontagent message type", "uri", req.getRequestURI());
            break;
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

    protected void reportResponse (HttpServletRequest req, Kontagent type, String tracking)
    {
        if (StringUtil.isBlank(tracking)) {
            log.warning("Missing tracking code for landing?", "uri", req.getRequestURI());
            reportLanding(req, Kontagent.OTHER_RESPONSE, "no_tracking:" + type.code, null);
            return;
        }
        reportLanding(req, type, type.code, tracking);
    }

    protected void reportLanding (
        HttpServletRequest req, Kontagent mtype, String ltype, String tracking)
    {
        boolean appAdded =
            ParameterUtil.isSet(req, "fb_sig") && ParameterUtil.isSet(req, "fb_sig_session_key");
        String recipId = StringUtil.getOr(
            req.getParameter("fb_sig_user"), req.getParameter("fb_sig_canvas_user"));
        _kontLogic.reportAction(mtype, "r", recipId, "i", appAdded ? "1" : "0", "tu", ltype,
                                (tracking.length() == 8) ? "su" : "u", tracking);
    }

    protected Random _rando = new Random();

    @Inject protected @Named(AppCodes.APPVERS) String _appvers;
    @Inject protected EverythingApp _app;
    @Inject protected KontagentLogic _kontLogic;
}
