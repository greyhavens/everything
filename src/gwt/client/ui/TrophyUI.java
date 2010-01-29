//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.data.TrophyData;

import client.ui.trophies.TrophyImages;

/**
 * Utility for displaying trophies.
 */
public class TrophyUI
{
    /**
     * Create a Trophy display widget.
     */
    public static Widget create (TrophyData trophy)
    {
        Image img = _trophies.trophy().createImage(); // TODO: one for each kind of trophy?
        img.setTitle(trophy.description);
        return Widgets.newFlowPanel(img, Widgets.newLabel(trophy.name, "Trophy"));
    }

    protected static final TrophyImages _trophies = GWT.create(TrophyImages.class);
}
