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

        // if this is an undirected landing, we need to generate a short tracking code
        if (type == Kontagent.OTHER_RESPONSE) {
            if (tracking != null) {
                log.warning("Got a tracking code but no Kontagent message type?",
                            "req", req.getRequestURI());
                // whatever, overwrite it with our short code
            }
            tracking = Integer.toHexString(_rando.nextInt());
            tracking = StringUtil.fill('0', 8-tracking.length()) + tracking;
        }

        // if we're the release candidate, use /candidate/everything/ otherwise use the versionless
        // URL and let GWT complain if someone makes an out of date service request (mostly things
        // are fine from version to version and not using the versioned URL means we don't start
        // 404ing after a release)
        String indexPath = "/everything/";
        if (_appvers.equals(AppCodes.RELEASE_CANDIDATE)) {
            indexPath = "/" + AppCodes.RELEASE_CANDIDATE + indexPath;
        }

        // if we have a tracking token, we need to pass that down to the app so that it can pass it
        // back up when it validates its session for the first time and we detect a new user and
        // report that the app was added
        if (tracking != null) {
            indexPath = "?t=" + tracking;
        }

        // otherwise pass the buck to the app servlet, it may have to get jiggy
        if (doFacebookAuth(req, rsp, _app.getFacebookAppURL(), null, indexPath)) {
            // if our authentication process actually directed the user to the app, we can emit our
            // event to Kontagent, otherwise we're in the middle of swizzling them and need to hold
            // off until the swizzling process is complete
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
            case POST:
                reportLanding(req, Kontagent.POST_RESPONSE, "stream", tracking);
                break;
            case OTHER_RESPONSE:
                reportLanding(req, Kontagent.OTHER_RESPONSE, vector, tracking);
                break;
            case APP_ADDED:
                // this is not a landing, the player has added the app after browsing something as
                // a guest; we pass their token back through to the client and it will supply it
                // when it refreshes its session
                break;
            default:
                log.warning("Got weird Kontagent message type", "uri", req.getRequestURI());
                break;
            }
        }
    }

    protected void reportResponse (HttpServletRequest req, Kontagent type, String tracking)
    {
        if (type != Kontagent.OTHER_RESPONSE && StringUtil.isBlank(tracking)) {
            log.warning("Missing tracking code for landing?", "uri", req.getRequestURI());
            type = Kontagent.OTHER_RESPONSE;
        }
        reportLanding(req, type, type.code, tracking);
    }

    protected void reportLanding (
        HttpServletRequest req, Kontagent type, String ltype, String tracking)
    {
        String appAdded = (ParameterUtil.isSet(req, "fb_sig") &&
                           ParameterUtil.isSet(req, "fb_sig_session_key")) ? "1" : "0";
        String rkey = (type == Kontagent.OTHER_RESPONSE) ? "s" : "r"; // retarded
        String recipId = StringUtil.getOr(req.getParameter("fb_sig_user"),
                                          req.getParameter("fb_sig_canvas_user"));
        String tkey = (tracking != null && tracking.length() == 8) ? "su" : "u";
        _kontLogic.reportAction(type, "tu", ltype, rkey, recipId, tkey, tracking, "i", appAdded);
    }

    protected Random _rando = new Random();

    @Inject protected @Named(AppCodes.APPVERS) String _appvers;
    @Inject protected EverythingApp _app;
    @Inject protected KontagentLogic _kontLogic;
}
