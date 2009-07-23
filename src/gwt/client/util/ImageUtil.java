//
// $Id$

package client.util;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Commands;

/**
 * Image related utility methods.
 */
public class ImageUtil
{
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
    public static Widget getMiniImageBox (String image, Command onClick)
    {
        return getImageBox(image, "miniImageBox", onClick);
    }

    protected static Widget getImageBox (String image, String styleName, Command onClick)
    {
        Widget clicky;
        if (image != null && image.length() > 0) {
            clicky = Widgets.newActionImage(S3_BUCKET + image, Commands.onClick(onClick));
        } else {
            clicky = Widgets.newActionLabel("", "Shim", Commands.onClick(onClick));
        }
        SmartTable wrap = new SmartTable(styleName, 0, 0);
        wrap.setWidget(0, 0, clicky);
        wrap.getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
        wrap.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_MIDDLE);
        return wrap;
    }

    /** The URL via which we load images from our Amazon S3 bucket. */
    protected static final String S3_BUCKET = "http://s3.amazonaws.com/everything.threerings.net/";
}
