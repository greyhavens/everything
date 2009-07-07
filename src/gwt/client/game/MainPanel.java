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

        add(new Label("Hello " + ctx.getMe().name));

        if (ctx.isAdmin()) {
            add(new Hyperlink("Admin bits", "admin"));
        }

        // TODO the main UI
    }

    public void setToken (String token)
    {
    }

    protected Context _ctx;

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
}
