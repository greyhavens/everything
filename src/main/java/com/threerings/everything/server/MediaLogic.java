//
// $Id$

package com.threerings.everything.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Map;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;

import com.threerings.s3.client.MediaType;
import com.threerings.s3.client.S3ByteArrayObject;
import com.threerings.s3.client.S3Connection;
import com.threerings.s3.client.S3Exception;
import com.threerings.s3.client.S3ServerException;
import com.threerings.s3.client.acl.AccessControlList;

import com.threerings.app.client.ServiceException;
import com.threerings.app.data.AppCodes;

import com.threerings.everything.data.Thing;

import static com.threerings.everything.Log.log;

/**
 * Provides media related services to server entities.
 */
@Singleton
public class MediaLogic
{
    /**
     * Downloads, scales and uploads the supplied image to our media repository.
     *
     * @return the path to the image relative to the repository.
     */
    public String processImage (URL imgurl)
        throws ServiceException
    {
        try {
            return processImage(ImageIO.read(imgurl));
        } catch (IOException ioe) {
            throw new ServiceException(ioe.getMessage());
        }
    }

    /**
     * Decodes, scales and uploads the supplied image to our media repository.
     *
     * @return the path to the image relative to the repository.
     */
    public String processImage (InputStream imgin)
        throws ServiceException
    {
        try {
            return processImage(ImageIO.read(imgin));
        } catch (IOException ioe) {
            throw new ServiceException(ioe.getMessage());
        }
    }

    /**
     * Scales and uploads the supplied image to our media repository.
     *
     * @return the path to the image relative to the repository.
     */
    public String processImage (BufferedImage image)
        throws ServiceException
    {
        // make sure our S3 bits are configured
        if (_app.getMediaStoreBucket() == null) {
            throw new ServiceException("e.no_s3_config");
        }

        try {
            if (image == null) {
                throw new ServiceException("e.unsupported_image_type");
            }

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
                           new S3ByteArrayObject(name, bout.toByteArray(), new MediaType(mimeType)),
                           AccessControlList.StandardPolicy.PUBLIC_READ, EXPIRES_2038);

            log.info("Processed thing image", "name", name,
                     "size", image.getWidth() + "x" + image.getHeight(),
                     "scaled", newWidth + "x" + newHeight);

            return name;

        } catch (S3ServerException e) {
            log.warning("S3 upload failed", "bucket", _app.getMediaStoreBucket(),
                        "code", e.getClass().getName(), "requestId", e.getRequestId(),
                        "hostId", e.getHostId(), "message", e.getMessage());
            throw ServiceException.internalError();

        } catch (S3Exception e) {
            log.warning("S3 upload failed", "bucket", _app.getMediaStoreBucket(), e);
            throw ServiceException.internalError();

        } catch (ServiceException se) {
            throw se; // no need to log this one

        } catch (Exception e) {
            log.warning("Failed to process uploaded imgae.", e);
            throw ServiceException.internalError();
        }
    }

    @Inject protected EverythingApp _app;

    // Effectively 'never' expire date.
    protected static final Map<String, String> EXPIRES_2038 =
        ImmutableMap.of("Expires", "Sun, 17 Jan 2038 19:14:07 GMT");
}
