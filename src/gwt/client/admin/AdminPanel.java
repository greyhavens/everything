//
// $Id$

package client.admin;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
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

        SmartTable links = new SmartTable(5, 0);
        links.setText(0, 0, "Admin:");
        links.setWidget(0, 1, new Hyperlink("Edit Things", "admin-edit"));
        links.setWidget(0, 2, new Hyperlink("Play Game", ""));
        add(links);
    }

    public void setToken (String token)
    {
        if (token.equals("edit")) {
            setContent(new EditThingsPanel(_ctx));
        } else {
            // TODO: download and display some overall statistics
        }
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
