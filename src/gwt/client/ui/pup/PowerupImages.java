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

    @Resource("all_new_cards.png")
    AbstractImagePrototype all_new_cards ();

    @Resource("all_collected_series.png")
    AbstractImagePrototype all_collected_series ();

    @Resource("ensure_one_vii.png")
    AbstractImagePrototype ensure_one_vii ();

    @Resource("ensure_one_viii.png")
    AbstractImagePrototype ensure_one_viii ();

    @Resource("ensure_one_ix.png")
    AbstractImagePrototype ensure_one_ix ();

    @Resource("ensure_one_x.png")
    AbstractImagePrototype ensure_one_x ();
}
