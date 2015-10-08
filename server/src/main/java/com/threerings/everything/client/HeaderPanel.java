//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.client;

import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.game.CoinLabel;
import com.threerings.everything.client.util.Args;
import com.threerings.everything.client.util.Context;
import com.threerings.everything.client.util.Page;

/**
 * Displays a simple header and navigation.
 */
public class HeaderPanel extends FlowPanel
{
    public HeaderPanel (Context ctx)
    {
        setStyleName("header");

        int col = 0;
        FluentTable bits = new FluentTable(0, 0, "Bits");
        if (ctx.getMe().isGuest()) {
            bits.at(0, col++).setHTML(ctx.getFacebookAddLink("Play Everything!"), "machine");
        } else {
            bits.at(0, col++).setText(getHello(ctx.getMe().name), "machine");
        }

        FlowPanel extras = Widgets.newFlowPanel("machine");
        if (ctx.isEditor()) {
            extras.add(Widgets.newHTML("&nbsp;&nbsp;", "inline"));
            extras.add(Args.createInlink("Taxonomy", Page.EDIT_CATS));
            extras.add(Widgets.newHTML("&nbsp;&nbsp;", "inline"));
            extras.add(Args.createInlink("Pending", Page.EDIT_PENDING));
            extras.add(Widgets.newHTML("&nbsp;&nbsp;", "inline"));
            extras.add(Args.createInlink("In Devel", Page.EDIT_INDEV));
        } else if (ctx.getGridsConsumed() >= Context.EXPERIENCED_GRIDS) {
            extras.add(Widgets.newHTML("&nbsp;&nbsp;", "inline"));
            extras.add(Args.createInlink("Editors", Page.EDIT_INTRO));
        }
        if (ctx.isAdmin()) {
            extras.add(Widgets.newHTML("&nbsp;&nbsp;", "inline"));
            extras.add(Args.createInlink("Bling", Page.GET_COINS, "admin"));
        }
        if (ctx.isMaintainer()) {
            extras.add(Widgets.newHTML("&nbsp;&nbsp;", "inline"));
            extras.add(Args.createInlink("Stats", Page.ADMIN_STATS));
            extras.add(Widgets.newHTML("&nbsp;&nbsp;", "inline"));
            extras.add(Args.createInlink("News", Page.ADMIN_NEWS));
        }

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

    protected static String getHello (String name)
    {
        switch (Random.nextInt(10)) {
        default:
        case 0: return "Hello: " + name;
        case 1: return "Hello, " + name + "!";
        case 2: return "Hi there, " + name;
        case 3: return "Howdy " + name + "!";
        case 4: return "Ahoy " + name;
        case 5: return "Greetings " + name;
        case 6: return "Yo " + name + "!";
        case 7: return "Hey " + name;
        case 8: return "You are: " + name;
        case 9: return "Mmm... pie.";
        }
    }
}
