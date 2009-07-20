//
// $Id$

package client.util;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

/**
 * Image related utility methods.
 */
public class ImageUtil
{
    /**
     * Returns the URL that can be used to display the supplied image. If the image is blank or
     * null, the unknown image will be shown.
     */
    public static String getImageURL (String image)
    {
        return (image == null || image.length() == 0) ? "images/unknown.png" : S3_BUCKET + image;
    }

    /**
     * Returns a widget that will display the specified card image, centered within it.
     */
    public static Widget getImageBox (String image)
    {
        return getImageBox(image, "imageBox", null);
    }

    /**
     * Returns a widget that will display the specified card image, scaled to mini-size, centered
     * within the box.
     */
    public static Widget getMiniImageBox (String image)
    {
        return getImageBox(image, "miniImageBox", null);
    }

    /**
     * Returns a widget that will display the specified card image, scaled to mini-size, centered
     * within the box.
     */
    public static Widget getMiniImageBox (String image, ClickHandler onClick)
    {
        return getImageBox(image, "miniImageBox", onClick);
    }

    protected static Widget getImageBox (String image, String styleName, ClickHandler onClick)
    {
        SmartTable table = new SmartTable(styleName, 0, 0);
        table.setWidget(0, 0, Widgets.newActionImage(getImageURL(image), onClick));
        table.getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
        table.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_MIDDLE);
        return table;
    }

    /** The URL via which we load images from our Amazon S3 bucket. */
    protected static final String S3_BUCKET = "http://s3.amazonaws.com/everything.threerings.net/";
}
