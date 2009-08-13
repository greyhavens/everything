//
// $Id$

package client;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

import client.game.CoinLabel;
import client.ui.XFBML;
import client.util.Args;
import client.util.Context;
import client.util.Page;

/**
 * Displays a simple header and navigation.
 */
public class HeaderPanel extends FlowPanel
{
    public HeaderPanel (Context ctx, String kontagentHello)
    {
        setStyleName("header");

        int col = 0;
        SmartTable bits = new SmartTable("Bits", 0, 0);
        if (ctx.getMe().isGuest()) {
            bits.setHTML(0, col++, ctx.getFacebookAddLink("Play Everything!"), 1, "machine");
        } else {
            bits.setText(0, col++, "Hello: " + ctx.getMe().name.toString(), 1, "machine");
        }

        FlowPanel extras = Widgets.newFlowPanel("machine");
        if (ctx.isEditor()) {
            extras.add(Widgets.newHTML("&nbsp;&nbsp;", "inline"));
            extras.add(Args.createInlink("Add Things", Page.EDIT_CATS));
        }
        if (ctx.isAdmin()) {
            extras.add(Widgets.newHTML("&nbsp;&nbsp;", "inline"));
            extras.add(Args.createInlink("Bling", Page.GET_COINS, "admin"));
        }
        if (ctx.isMaintainer()) {
            extras.add(Widgets.newHTML("&nbsp;&nbsp;", "inline"));
            extras.add(Args.createInlink("Stats", Page.STATS));
            extras.add(Widgets.newHTML("&nbsp;&nbsp;", "inline"));
            extras.add(Args.createInlink("Players", Page.PLAYERS));
            extras.add(Widgets.newHTML("&nbsp;&nbsp;", "inline"));
            extras.add(Args.createInlink("News", Page.NEWS));
        }
        // add our kontagent "page request" pixel to the extras
        extras.add(Widgets.newImage(kontagentHello));
        bits.setWidget(0, col, extras);
        bits.getFlexCellFormatter().setHorizontalAlignment(0, col++, HasAlignment.ALIGN_CENTER);

        bits.setWidget(0, col, new CoinLabel("You have: ", ctx.getCoins()));
        bits.getFlexCellFormatter().setHorizontalAlignment(0, col++, HasAlignment.ALIGN_RIGHT);
        add(bits);

        SmartTable links = new SmartTable("Links", 8, 0);
        col = 0;
        links.setWidget(1, col++, Args.createLink("News", Page.LANDING), 1, "machine");
        links.setWidget(1, col++, Args.createLink("Flip Cards", Page.FLIP), 1, "machine");
        links.setWidget(1, col++, Args.createLink("Your Collection", Page.BROWSE), 1, "machine");
        links.setWidget(1, col++, Args.createLink("Shop", Page.SHOP), 1, "machine");
        links.setWidget(1, col++, Args.createLink("Get Coins", Page.GET_COINS), 1, "machine");
        links.setWidget(1, col++, Args.createLink("Friends", Page.FRIENDS), 1, "machine");
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
