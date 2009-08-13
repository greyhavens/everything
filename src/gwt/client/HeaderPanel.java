//
// $Id$

package client;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.FluentTable;
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
        FluentTable bits = new FluentTable(0, 0, "Bits");
        if (ctx.getMe().isGuest()) {
            bits.at(0, col++).setHTML(ctx.getFacebookAddLink("Play Everything!"), "machine");
        } else {
            bits.at(0, col++).setText("Hello: " + ctx.getMe().name, "machine");
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
        bits.at(0, col++).setWidget(extras).alignCenter();

        bits.at(0, col++).setWidget(new CoinLabel("You have: ", ctx.getCoins())).alignRight();
        add(bits);

        FluentTable links = new FluentTable(8, 0, "Links");
        links.add().setWidget(Args.createLink("News", Page.LANDING), "machine").
            right().setWidget(Args.createLink("Flip Cards", Page.FLIP), "machine").
            right().setWidget(Args.createLink("Your Collection", Page.BROWSE), "machine").
            right().setWidget(Args.createLink("Shop", Page.SHOP), "machine").
            right().setWidget(Args.createLink("Get Coins", Page.GET_COINS), "machine").
            right().setWidget(Args.createLink("Friends", Page.FRIENDS), "machine").
            right().setWidget(Args.createLink("Credits", Page.CREDITS), "machine");
        add(links);
    }

    @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        XFBML.parse(this);
    }
}
