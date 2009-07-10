//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.client.EverythingServiceAsync;
import com.threerings.everything.data.SessionData;

import client.util.Context;
import client.util.PanelCallback;

/**
 * Contains the main game UI.
 */
public class MainPanel extends FlowPanel
{
    public MainPanel (Context ctx)
    {
        setStyleName("main");
        _ctx = ctx;

        SmartTable links = new SmartTable(5, 0);
        links.setText(0, 0, "Hello:");
        links.setText(0, 1, ctx.getMe().name);
        if (ctx.isAdmin()) {
            links.setWidget(0, 2, new Hyperlink("Create Sets", "admin-edit"));
        }
        add(links);

        // for now display the player's current grid by default
        add(new GridPanel(ctx));
    }

    public void setToken (String token)
    {
    }

    protected Context _ctx;

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
}
