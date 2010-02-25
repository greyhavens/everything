//
// $Id$

package com.threerings.everything.server;

import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.google.common.io.Files;

import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.FileUtil;

import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.AppServlet;

import static com.threerings.everything.Log.log;

/**
 * Generates trophy snapshot images for Facebook feed posts, etc.
 *
 * TODO: this is not actually in-use yet, as we just use the trophy image.
 */
public class TrophyImageServlet extends AppServlet
{
    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        String trophyId = ParameterUtil.getParameter(req, "trophy", "unknown_trophy");
        String filename = trophyToFile(trophyId);
        File trophy = FileUtil.newFile(_approot, "web", "images", "trophies", filename);
        try {
            rsp.setContentType("image/png");
            Files.copy(trophy, rsp.getOutputStream());

        } catch (IOException ioe) {
            log.warning("Failed to output trophy image",
                "trophyId", trophyId, "file", filename, ioe);
        }
    }

    /**
     * Turn a trophyId to a filename in the trophies directory.
     */
    protected String trophyToFile (String trophyId)
    {
        // for now, all trophies use the same image
        return "trophy.png";
    }

    @Inject protected @Named(AppCodes.APPROOT) File _approot;
}
