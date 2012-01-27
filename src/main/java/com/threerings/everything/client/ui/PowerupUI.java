//
// $Id$

package com.threerings.everything.client.ui;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
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
        AbstractImagePrototype pimg = _picons.get(pup);
        return (pimg == null) ? Widgets.newShim(26, 26) : pimg.createImage();
    }

    protected static final PowerupImages _pupimgs = GWT.create(PowerupImages.class);

    protected static Map<Powerup, AbstractImagePrototype> _picons =
        new HashMap<Powerup, AbstractImagePrototype>();
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
