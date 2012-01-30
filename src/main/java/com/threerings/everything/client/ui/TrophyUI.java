//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.data.TrophyData;

import com.threerings.everything.client.game.TrophyDialog;
import com.threerings.everything.client.util.Context;

/**
 * Utility for displaying trophies.
 */
public class TrophyUI
{
    /**
     * Returns the image to display for the supplied trophy. If the trophy belongs to the caller
     * (isMine) it may be clicked to post to their feed.
     */
    public static Image getTrophyImage (Context ctx, TrophyData trophy, boolean isMine)
    {
        Image img = new Image("images/trophies/trophy.png"); // TODO: one for each kind of trophy
        img.setTitle(trophy.description);
        if (isMine) {
            Widgets.makeActionable(img, TrophyDialog.makeHandler(ctx, trophy), null);
        }
        return img;
    }

    /**
     * Creates a simple UI to display a trophy.
     */
    public static Widget create (Context ctx, TrophyData trophy, boolean isMine)
    {
        Image img = getTrophyImage(ctx, trophy, isMine);
        return Widgets.newFlowPanel(img, Widgets.newLabel(trophy.name, "Trophy"));
    }
}
