//
// $Id$

package client.admin;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

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
        buttons.add(new Button("Edit Things", new ClickHandler() {
            public void onClick (ClickEvent event) {
                setContent(new EditThingsPanel(_ctx));
            }
        }));
        buttons.add(Widgets.newShim(5, 5));
        buttons.add(new Button("TODO", new ClickHandler() {
            public void onClick (ClickEvent event) {
                // TODO
            }
        }));
        add(buttons);

        // TODO: download and display some overall statistics
    }

    protected void setContent (Widget content)
    {
        if (_content != null) {
            remove(_content);
        }
        add(_content = content);
    }

    protected Context _ctx;
    protected Widget _content;
}
