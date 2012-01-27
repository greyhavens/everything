//
// $Id$

package com.threerings.everything.server;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.swing.Label;
import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.FileUtil;

import com.threerings.app.server.AppServlet;

import com.threerings.everything.data.Thing;
import com.threerings.everything.server.persist.ThingRepository;
import static com.threerings.everything.Log.log;

/**
 * Generates card snapshot images for Facebook feed posts, etc.
 */
public class CardImageServlet extends AppServlet
{
    @Override // from HttpServlet
    public void init ()
    {
        // load up our card front image
        File cback = FileUtil.newFile(_approot, "images", "card_front.png");
        try {
            _cback = ImageIO.read(cback);
        } catch (IOException ioe) {
            log.warning("Failed to load card front image", "path", cback, ioe);
        }

        // load up our custom fonts
        _sfont = loadFont("josschrift.ttf");
        _mfont = loadFont("copper.ttf");
    }

    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        String thingId = ParameterUtil.getParameter(req, "thing", "0");
        Thing thing = null;
        try {
            thing = _thingRepo.loadThing(Integer.parseInt(thingId));;
        } catch (Exception e) {
            log.warning("Failed to obtain thing for card image", "id", thingId, e);
        }

        BufferedImage image = new BufferedImage(_cback.getWidth(), _cback.getHeight(),
                                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D gfx = image.createGraphics();
        try {
            gfx.drawImage(_cback, 0, 0, null);

            if (thing != null) {
                BufferedImage timage = ImageIO.read(new URL(S3_BUCKET + thing.image));
                int swidth = timage.getWidth()/3, sheight = timage.getHeight()/3;
                AffineTransform xform = AffineTransform.getTranslateInstance(
                    CARD.x + (CARD.width - swidth)/2, CARD.y + (CARD.height - sheight)/2);
                xform.scale(1/3f, 1/3f);
                gfx.drawRenderedImage(timage, xform);

                SwingUtil.activateAntiAliasing(gfx);

                Label name = new Label(thing.name, TEXT_COLOR, _sfont.deriveFont(Font.PLAIN, 14));
                name.setAlignment(Label.CENTER);
                name.setTargetWidth(CARD.width);
                name.layout(gfx);
                int x = CARD.x + (CARD.width - name.getSize().width)/2;
                name.render(gfx, x, 5);

                Label brand = new Label("Everything!", TEXT_COLOR,
                                        _mfont.deriveFont(Font.PLAIN, 14));
                brand.layout(gfx);
                x = CARD.x + (CARD.width - brand.getSize().width)/2;
                brand.render(gfx, x, CARD.y + CARD.height - brand.getSize().height - 5);
            }

        } catch (Exception e) {
            log.warning("Failed while generating card image", "thingId", thingId, e);

        } finally {
            gfx.dispose();
        }

        rsp.setContentType("image/png");
        ImageIO.write(image, "png", rsp.getOutputStream());
    }

    protected Font loadFont (String file)
    {
        File font = FileUtil.newFile(_approot, file);
        try {
            return Font.createFont(Font.TRUETYPE_FONT, font);
        } catch (Exception e) {
            log.warning("Failed to load custom font", "path", font, e);
            return new Font("Dialog", Font.PLAIN, 14);
        }
    }

    protected BufferedImage _cback;
    protected Font _sfont, _mfont;

    @Inject protected @Named(EverythingApp.APPROOT) File _approot;
    @Inject protected ThingRepository _thingRepo;

    protected static final Color TEXT_COLOR = new Color(0x442D17);
    protected static final Rectangle CARD = new Rectangle(2, 2, 125, 150);

    /** The URL via which we load images from our Amazon S3 bucket. */
    protected static final String S3_BUCKET = "http://s3.amazonaws.com/everything.threerings.net/";
}
