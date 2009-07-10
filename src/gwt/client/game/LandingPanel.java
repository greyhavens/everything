//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;

import client.util.Context;

/**
 * The main interface displayed when the player arrives at the game.
 */
public class LandingPanel extends FlowPanel
{
    public LandingPanel (Context ctx)
    {
        // TODO: display the game news?
        add(Widgets.newLabel("Welcome to Everything!", "infoLabel"));
    }
}
