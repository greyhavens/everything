//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.ui;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.data.Powerup;

import com.threerings.everything.client.ui.pup.PowerupImages;

/**
 * Powerup related user interface bits.
 */
public class PowerupUI
{
    /**
     * Creates an icon for the supplied powerup.
     */
    public static Widget newIcon (Powerup pup)
    {
        ImageResource pimg = _picons.get(pup);
        return (pimg == null) ? Widgets.newShim(26, 26) : new Image(pimg);
    }

    protected static final PowerupImages _pupimgs = GWT.create(PowerupImages.class);

    protected static Map<Powerup, ImageResource> _picons =
        new HashMap<Powerup, ImageResource>();
    static {
        _picons.put(Powerup.SHOW_CATEGORY, _pupimgs.show_category());
        _picons.put(Powerup.SHOW_SUBCATEGORY, _pupimgs.show_subcategory());
        _picons.put(Powerup.SHOW_SERIES, _pupimgs.show_series());
        _picons.put(Powerup.EXTRA_FLIP, _pupimgs.extra_flip());
        _picons.put(Powerup.ALL_NEW_CARDS, _pupimgs.all_new_cards());
        _picons.put(Powerup.ALL_COLLECTED_SERIES, _pupimgs.all_collected_series());
        _picons.put(Powerup.ENSURE_ONE_VII, _pupimgs.ensure_one_vii());
        _picons.put(Powerup.ENSURE_ONE_VIII, _pupimgs.ensure_one_viii());
        _picons.put(Powerup.ENSURE_ONE_IX, _pupimgs.ensure_one_ix());
        _picons.put(Powerup.ENSURE_ONE_X, _pupimgs.ensure_one_x());
    }
}
