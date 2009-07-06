//
// $Id$

package client.admin;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;

import client.util.Context;

/**
 * Displays the main admin interface.
 */
public class AdminPanel extends FlowPanel
{
    public AdminPanel (Context ctx)
    {
        setStyleName("admin");
        _ctx = ctx;

        HorizontalPanel buttons = new HorizontalPanel();
    }

    protected Context _ctx;
}
