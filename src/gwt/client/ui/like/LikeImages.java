//
// $Id$

package client.ui.like;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Images used on like widget.
 */
public interface LikeImages extends ImageBundle
{
    @Resource("good.gif")
    AbstractImagePrototype good ();

    @Resource("neutral.gif")
    AbstractImagePrototype neutral ();

    @Resource("bad.jpg")
    AbstractImagePrototype bad ();

    @Resource("good_selected.gif")
    AbstractImagePrototype good_selected ();

    @Resource("neutral_selected.gif")
    AbstractImagePrototype neutral_selected ();

    @Resource("bad_selected.jpg")
    AbstractImagePrototype bad_selected ();
}
