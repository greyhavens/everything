//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.FlowPanel;
import com.threerings.gwt.ui.Widgets;

import client.util.Context;

/**
 * Displays a request to add our app.
 */
public class AddAppPanel extends FlowPanel
{
    public AddAppPanel (Context ctx, boolean asPage)
    {
        setStyleName("addApp");
        if (asPage) {
            addStyleName("page");
        }
        add(Widgets.newHTML("Click " + ctx.getFacebookAddLink("Add Everything") +
                            " to add the Everything app and start playing!", "machine"));
    }
}
