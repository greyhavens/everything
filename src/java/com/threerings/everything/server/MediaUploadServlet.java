//
// $Id$

package com.threerings.everything.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.threerings.s3.client.S3ByteArrayObject;
import com.threerings.s3.client.S3Connection;
import com.threerings.s3.client.S3Exception;
import com.threerings.s3.client.S3ServerException;
import com.threerings.s3.client.acl.AccessControlList;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.StringUtil;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.server.AppServlet;
import com.threerings.user.OOOUser;

import com.threerings.everything.data.Thing;

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
            OOOUser admin = requireAdmin(req, rsp);

            // validate the content length is sane
            if (req.getContentLength() <= 0) {
                throw new FileUploadException("Negative content length?");
            }

            // make sure our S3 bits are configured
            if (_app.getMediaStoreBucket() == null) {
                throw new ServiceException("e.no_s3_config");
            }

            // locate our file among the file items
            file = extractFile(req);
            if (file == null) {
                log.warning("Failed to extract file from upload request.", "req", req);
                throw new ServiceException("e.internal_error");
            }

            // convert the image to our desired format, possibly scaling it in the process
            BufferedImage image = ImageIO.read(file.getInputStream());

            float tratio = Thing.MAX_IMAGE_WIDTH / (float)Thing.MAX_IMAGE_HEIGHT;
            float iratio = image.getWidth() / (float)image.getHeight();
            float scale = iratio > tratio ? Thing.MAX_IMAGE_WIDTH / (float)image.getWidth() :
                Thing.MAX_IMAGE_HEIGHT / (float)image.getHeight();
            int newWidth = Math.max(1, Math.round(scale * image.getWidth()));
            int newHeight = Math.max(1, Math.round(scale * image.getHeight()));

            boolean trans = (image.getColorModel().getTransparency() != Transparency.OPAQUE);

            // generate the scaled image
            BufferedImage timage = new BufferedImage(
                newWidth, newHeight,
                trans ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            Graphics2D gfx = timage.createGraphics();
            try {
                Image scaledImg = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                gfx.drawImage(scaledImg, 0, 0, null);
                scaledImg.flush();
            } finally {
                gfx.dispose();
            }

            // reencode the image and compute its digest
            MessageDigest digest = MessageDigest.getInstance("SHA");
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            String format = trans ? "png" : "jpg";
            ImageIO.write(timage, format, new DigestOutputStream(bout, digest));

            // name it based on its hex data plus its digest
            String name = StringUtil.hexlate(digest.digest()) + "." + format;
            String mimeType = trans ? "image/png" : "image/jpeg";

            // ship it up to S3
            S3Connection conn = new S3Connection(_app.getMediaStoreId(), _app.getMediaStoreKey());
            conn.putObject(_app.getMediaStoreBucket(),
                           new S3ByteArrayObject(name, bout.toByteArray(), mimeType),
                           AccessControlList.StandardPolicy.PUBLIC_READ, EXPIRES_2038);

            log.info("Processed thing image", "name", name,
                     "size", image.getWidth() + "x" + image.getHeight(),
                     "scaled", newWidth + "x" + newHeight);

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

        } catch (S3ServerException e) {
            log.warning("S3 upload failed", "bucket", _app.getMediaStoreBucket(),
                        "code", e.getClass().getName(), "requestId", e.getRequestId(),
                        "hostId", e.getHostId(), "message", e.getMessage());
            displayError(rsp, "e.internal_error");

        } catch (S3Exception e) {
            log.warning("S3 upload failed", "bucket", _app.getMediaStoreBucket(), e);
            displayError(rsp, "e.internal_error");

        } catch (ServiceException se) {
            displayError(rsp, se.getMessage());

        } catch (Exception e) {
            log.warning("Failed to process uploaded imgae.", e);
            displayError(rsp, "e.internal_error");

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

//     /**
//      * Copy the supplied file to S3
//      */
//     protected static boolean copyFileToS3 (File file, String mimeType, String name,
//         String subdirectory, Map<String, String> headers)
//         throws IOException
//     {

//         } catch (S3ServerException e) {
//             // S3 Server-side Exception
//             log.warning("S3 upload failed [code=" + e.getClass().getName() +
//                         ", requestId=" + e.getRequestId() + ", hostId=" + e.getHostId() +
//                         ", message=" + e.getMessage() + "].");
//             return false;

//         } catch (S3Exception e) {
//             // S3 Client-side Exception
//             log.warning("S3 upload failed: " + e);
//             return false;
//         }
//     }

    // dependencies
    @Inject protected EverythingApp _app;

    protected static final int MEGABYTE = 1024 * 1024;
    protected static final int MAX_UPLOAD_SIZE = 2 * MEGABYTE;

    // Effectively 'never' expire date.
    protected static final Map<String, String> EXPIRES_2038 =
        ImmutableMap.of("Expires", "Sun, 17 Jan 2038 19:14:07 GMT");
}
