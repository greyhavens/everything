//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.client.util;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

/**
 * Image related utility methods.
 */
public class ImageUtil
{
    /**
     * Returns the URL for the supplied image.
     */
    public static String getImageURL (String image)
    {
        return S3_BUCKET + image;
    }

    /**
     * Returns a widget that will display the specified card image, centered within it.
     */
    public static Widget getImageBox (String image)
    {
        return getImageBox(image, "imageBox", null, null);
    }

    /**
     * Returns a widget that will display the specified card image, scaled to mini-size, centered
     * within the box.
     */
    public static Widget getMiniImageBox (String image)
    {
        return getImageBox(image, "miniImageBox", null, null);
    }

    /**
     * Returns a widget that will display the specified card image, scaled to mini-size, centered
     * within the box.
     */
    public static Widget getMiniImageBox (
        String image, ClickHandler onClick, Value<Boolean> enabled)
    {
        return getImageBox(image, "miniImageBox", onClick, enabled);
    }

    protected static Widget getImageBox (
        String image, String styleName, ClickHandler onClick, Value<Boolean> enabled)
    {
        Widget clicky;
        if (image != null && image.length() > 0) {
            clicky = Widgets.makeActionable(Widgets.newImage(getImageURL(image)), onClick, enabled);
        } else {
            clicky = Widgets.makeActionable(Widgets.newLabel("", "Shim"), onClick, enabled);
        }
        FluentTable wrap = new FluentTable(0, 0, styleName);
        wrap.add().setWidget(clicky).alignCenter().alignMiddle();
        return wrap;
    }

    /** The URL via which we load images from our Amazon S3 bucket. */
    protected static final String S3_BUCKET = "http://s3.amazonaws.com/everything.threerings.net/";
}
