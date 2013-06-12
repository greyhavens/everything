//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a widget with a background which contains a transparent shadow around the edge. This
 * involves fiddling because the background actually contains the shadow, but we want to display a
 * solid background color in the non-transparent parts of the image while the image is loading.
 */
public class ShadowPanel extends SimplePanel
{
    public ShadowPanel (Widget contents, String bgimage, String bgcolor,
                        int shadowTop, int shadowRight, int shadowBottom, int shadowLeft)
    {
        setWidget(contents);
        DOM.setStyleAttribute(getElement(), "background", "url(" + bgimage + ") no-repeat");
        DOM.setStyleAttribute(getElement(), "padding", shadowTop + "px " + shadowRight + "px " +
                              shadowBottom + "px " + shadowLeft + "px");
        DOM.setStyleAttribute(contents.getElement(), "backgroundColor", bgcolor);
        DOM.setStyleAttribute(contents.getElement(), "backgroundImage", "url(" + bgimage + ")");
        DOM.setStyleAttribute(contents.getElement(), "backgroundRepeat", "no-repeat");
        DOM.setStyleAttribute(contents.getElement(), "backgroundPosition",
                              "-" + shadowLeft + "px -" + shadowTop + "px");
    }
}
