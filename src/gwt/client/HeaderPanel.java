//
// $Id$

package client;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.data.Build;

import client.ui.XFBML;
import client.util.Args;
import client.util.Context;
import client.util.Page;

/**
 * Displays a simple header and navigation.
 */
public class HeaderPanel extends FlowPanel
{
    public HeaderPanel (Context ctx)
    {
        setStyleName("header");

        int col = 0;
        SmartTable bits = new SmartTable("Bits", 0, 0);
        if (ctx.getMe().isGuest()) {
            bits.setHTML(0, col++, ctx.getFacebookAddLink("Play Everything!"), 1, "machine");
        } else {
            bits.setText(0, col++, "Hello: " + ctx.getMe().name.toString(), 1, "machine");
        }
        if (ctx.isAdmin()) {
            bits.setWidget(0, col, Args.createLink("Dashboard", Page.DASHBOARD), 1, "machine");
            bits.getFlexCellFormatter().setHorizontalAlignment(0, col++, HasAlignment.ALIGN_CENTER);
        }
        bits.setText(0, col, "Build: " + Build.time(), 1, "machine");
        bits.getFlexCellFormatter().setHorizontalAlignment(0, col++, HasAlignment.ALIGN_RIGHT);
        add(bits);

        SmartTable links = new SmartTable("Links", 5, 0);
        col = 0;
        links.setWidget(1, col++, Args.createLink("News", Page.LANDING), 1, "machine");
        links.setWidget(1, col++, Args.createLink("Flip Cards", Page.FLIP), 1, "machine");
        links.setWidget(1, col++, Args.createLink("Your Collection", Page.BROWSE), 1, "machine");
        links.setWidget(1, col++, Args.createLink("Shop", Page.SHOP), 1, "machine");
        links.setWidget(1, col++, Args.createLink("Friends", Page.FRIENDS), 1, "machine");
        if (ctx.isEditor()) {
            links.setWidget(1, col++, Args.createLink("Add Things", Page.EDIT_CATS), 1, "machine");
        }
        links.setWidget(1, col++, Args.createLink("Credits", Page.CREDITS), 1, "machine");
        add(links);
    }

    @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        XFBML.parse(this);
    }
}
