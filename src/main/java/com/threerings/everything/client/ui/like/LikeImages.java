//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.ui.like;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Images used on like widget.
 */
public interface LikeImages extends ImageBundle
{
    @Resource("pos.png")
    AbstractImagePrototype pos ();

    @Resource("neg.png")
    AbstractImagePrototype neg ();

    @Resource("pos_sel.png")
    AbstractImagePrototype pos_selected ();

    @Resource("neg_sel.png")
    AbstractImagePrototype neg_selected ();
}
