//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.data.TrophyData;

import client.game.TrophyDialog;
import client.util.Context;

/**
 * Utility for displaying trophies.
 */
public class TrophyUI
{
    /**
     * Create a Trophy display widget.
     */
    public static Widget create (Context ctx, TrophyData trophy, boolean isMine)
    {
        Image img = new Image("images/trophies/trophy.png"); // TODO: one for each kind of trophy
        img.setTitle(trophy.description);
        if (isMine) {
            Widgets.makeActionable(img, TrophyDialog.makeHandler(ctx, trophy), null);
        }
        return Widgets.newFlowPanel(img, Widgets.newLabel(trophy.name, "Trophy"));
    }
}
