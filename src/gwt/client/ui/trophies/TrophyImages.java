//
// $Id$

package client.ui.trophies;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Trophy images.
 */
public interface TrophyImages extends ImageBundle
{
    /** For now, the single trophy used everywhere. */
    @Resource("trophy.png")
    AbstractImagePrototype trophy ();
}
