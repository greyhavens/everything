//
// $Id$

package client.admin;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;

import com.threerings.gwt.ui.Widgets;

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
        buttons.add(new Button("Create Categories", new ClickHandler() {
            public void onClick (ClickEvent event) {
                // TODO
            }
        }));
        buttons.add(Widgets.newShim(5, 5));
        buttons.add(new Button("Create Sets", new ClickHandler() {
            public void onClick (ClickEvent event) {
                // TODO
            }
        }));
        add(buttons);

        // TODO: download and display some overall statistics
    }

    protected Context _ctx;
}
