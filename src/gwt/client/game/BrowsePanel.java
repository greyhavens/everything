//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;

import client.util.Context;

/**
 * Displays a player's collection.
 */
public class BrowsePanel extends FlowPanel
{
    public BrowsePanel (Context ctx)
    {
        add(Widgets.newLabel("Coming soon!", "infoLabel"));
    }
}
