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
    @Resource("pos.png")
    AbstractImagePrototype pos ();

    @Resource("neu.png")
    AbstractImagePrototype neu ();

    @Resource("neg.png")
    AbstractImagePrototype neg ();

    @Resource("pos_sel.png")
    AbstractImagePrototype pos_selected ();

    @Resource("neu_sel.png")
    AbstractImagePrototype neu_selected ();

    @Resource("neg_sel.png")
    AbstractImagePrototype neg_selected ();
}
