//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.ui.like;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

/**
 * Images used on like widget.
 */
public interface LikeImages extends ClientBundle
{
    @Source("pos.png")
    ImageResource pos ();

    @Source("neg.png")
    ImageResource neg ();

    @Source("pos_sel.png")
    ImageResource pos_selected ();

    @Source("neg_sel.png")
    ImageResource neg_selected ();
}
