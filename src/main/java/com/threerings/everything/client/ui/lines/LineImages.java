//
// $Id$

package com.threerings.everything.client.ui.lines;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Taxonomy line images.
 */
public interface LineImages extends ImageBundle
{
    @Resource("downleft.png")
    AbstractImagePrototype downleft ();

    @Resource("downright.png")
    AbstractImagePrototype downright ();

    @Resource("downleftright.png")
    AbstractImagePrototype downleftright ();

    @Resource("upleftright.png")
    AbstractImagePrototype upleftright ();

    @Resource("updownleft.png")
    AbstractImagePrototype updownleft ();

    @Resource("updownright.png")
    AbstractImagePrototype updownright ();

    @Resource("upleft.png")
    AbstractImagePrototype upleft ();

    @Resource("upright.png")
    AbstractImagePrototype upright ();

    @Resource("leftright.png")
    AbstractImagePrototype leftright ();

    @Resource("updown.png")
    AbstractImagePrototype updown ();

    @Resource("full.png")
    AbstractImagePrototype full ();
}
