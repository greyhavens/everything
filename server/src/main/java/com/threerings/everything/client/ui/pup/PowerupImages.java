//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.ui.pup;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

/**
 * Powerup images.
 */
public interface PowerupImages extends ClientBundle
{
    @Source("show_category.png")
    ImageResource show_category ();

    @Source("show_subcategory.png")
    ImageResource show_subcategory ();

    @Source("show_series.png")
    ImageResource show_series ();

    @Source("extra_flip.png")
    ImageResource extra_flip ();

    @Source("all_new_cards.png")
    ImageResource all_new_cards ();

    @Source("all_collected_series.png")
    ImageResource all_collected_series ();

    @Source("ensure_one_vii.png")
    ImageResource ensure_one_vii ();

    @Source("ensure_one_viii.png")
    ImageResource ensure_one_viii ();

    @Source("ensure_one_ix.png")
    ImageResource ensure_one_ix ();

    @Source("ensure_one_x.png")
    ImageResource ensure_one_x ();
}
