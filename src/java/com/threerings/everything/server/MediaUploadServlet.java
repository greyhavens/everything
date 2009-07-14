//
// $Id$

package com.threerings.everything.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.samskivert.io.StreamUtil;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.AppServlet;

import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;

import static com.threerings.everything.Log.log;

/**
 * Handles uploading media for Everything.
 */
public class MediaUploadServlet extends AppServlet
{
    @Override // from HttpServlet
    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        FileItem file = null;
        try {
            PlayerRecord player = _playerRepo.loadPlayer(requireUser(req, rsp).userId);
            if (player == null || !player.isEditor) {
                throw new ServiceException(AppCodes.E_ACCESS_DENIED);
            }

            // validate the content length is sane
            if (req.getContentLength() <= 0) {
                throw new FileUploadException("Negative content length?");
            }

            // locate our file among the file items
            file = extractFile(req);
            if (file == null) {
                log.warning("Failed to extract file from upload request.", "req", req);
                throw new ServiceException("e.internal_error");
            }

            // decode the uploaded image
            String name = _mediaLogic.processImage(file.getInputStream());

            // write out the magical incantations that are needed to cause our magical little
            // frame to communicate the newly assigned image name to the MediaUploader widget
            PrintStream out = null;
            try {
                out = new PrintStream(rsp.getOutputStream());
                out.println("<html>");
                out.println("<head></head>");
                String script = "parent.uploadComplete('" + name + "')";
                out.println("<body onLoad=\"" + script + "\"></body>");
                out.println("</html>");
            } finally {
                StreamUtil.close(out);
            }

        } catch (ServletFileUpload.SizeLimitExceededException slee) {
            log.info(slee.getMessage(), "size", slee.getActualSize(),
                     "allowed", slee.getPermittedSize());
            displayError(rsp, "e.upload_too_large");

        } catch (FileUploadException fue) {
            log.info("File upload failed", "error", fue.getMessage());
            displayError(rsp, "e.internal_error");

        } catch (ServiceException se) {
            displayError(rsp, se.getMessage());

        } finally {
            // delete the temporary uploaded file data
            if (file != null) {
                file.delete();
            }
        }
    }

    /**
     * Parse the upload request and returns the first FileItem that is an actual file. Returns null
     * if no file was found.
     */
    protected FileItem extractFile (HttpServletRequest req)
        throws FileUploadException
    {
        // TODO: create a custom file item factory that just puts items in the right place from the
        // start and computes the SHA hash on the way
        ServletFileUpload upload = new ServletFileUpload(
            new DiskFileItemFactory(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD,
                                    new File(System.getProperty("java.io.tmpdir"))));

        // enforce maximum sizes for the client supplied content size and the temporary file
        upload.setSizeMax(MAX_UPLOAD_SIZE);
        upload.setFileSizeMax(MAX_UPLOAD_SIZE);

        // the first thing that looks like a file wins
        for (Object obj : upload.parseRequest(req)) {
            if (!((FileItem)obj).isFormField()) {
                return (FileItem)obj;
            }
        }
        return null;
    }

    /**
     * Reports an error to the caller by returning HTML that calls a JavaScript error method on the
     * client, passing in the supplied error code.
     */
    protected void displayError (HttpServletResponse rsp, String ecode)
    {
        PrintStream out = null;
        try {
            out = new PrintStream(rsp.getOutputStream());
            out.println("<html>");
            out.println("<head></head>");
            out.println("<body onLoad=\"parent.uploadError('" + ecode + "');\"></body>");
            out.println("</html>");

        } catch (IOException ioe) {
            log.warning("Failed to setup OutputStream when displaying error.", ioe);

        } finally {
            StreamUtil.close(out);
        }
    }

    // dependencies
    @Inject protected MediaLogic _mediaLogic;
    @Inject protected PlayerRepository _playerRepo;

    protected static final int MEGABYTE = 1024 * 1024;
    protected static final int MAX_UPLOAD_SIZE = 2 * MEGABYTE;
}
