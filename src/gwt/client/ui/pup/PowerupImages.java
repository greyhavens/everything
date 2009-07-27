//
// $Id$

package client.ui.pup;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Powerup images.
 */
public interface PowerupImages extends ImageBundle
{
    @Resource("show_category.png")
    AbstractImagePrototype show_category ();

    @Resource("show_subcategory.png")
    AbstractImagePrototype show_subcategory ();

    @Resource("show_series.png")
    AbstractImagePrototype show_series ();

    @Resource("extra_flip.png")
    AbstractImagePrototype extra_flip ();
}
