//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;

import com.threerings.everything.data.CoinPrices;
import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Console;

import com.threerings.everything.client.ui.bling.BlingImages;
import com.threerings.everything.client.util.Context;

/**
 * Displays an interface for buying coins via Facebook Credits.
 */
public class BuyCoinsPage extends FlowPanel
{
    public BuyCoinsPage (final Context ctx, String action)
    {
        addStyleName("buyCoins");
        addStyleName("page");
        add(Widgets.newLabel(_msgs.selectHeader(), "Header", "machine"));

        FluentTable offers = new FluentTable(15, 0, "Offers");
        offers.add().setText(_msgs.selectOffer(), "machine").setColSpan(4);

        for (final CoinPrices.Offer offer : CoinPrices.OFFERS) {
            if (offer.state != CoinPrices.State.ACTIVE) {
                continue; // don't display inactive offers
            }
            PushButton button = Widgets.newPushButton(
                new Image(_images.buywithfb_up()),
                new Image(_images.buywithfb_up()),
                new Image(_images.buywithfb_down()), new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        startPurchase(offer.graphURL(ctx.backendURL()));
                    }
                });
            button.setStyleName("BuyButton");
            String cost = "$" + (offer.pennies / 100) + "." + (offer.pennies % 100);
            offers.add().setWidget(new CoinLabel(offer.coins)).
                right().setText(_msgs.buyFor()).
                right().setWidget(Widgets.newInlineLabel(cost, "machine")).
                right().setWidget(button);
        }
        add(offers);
    }

    protected void onPurchaseComplete (String status)
    {
        Console.log("Purchase complete '" + status + "'");
    }

    protected native void startPurchase (String offerURL) /*-{
        $wnd.FB.ui({
          method: 'pay',
          action: 'purchaseitem',
          product: offerURL,
          quantity: 1
        }, function(data) {
            this.@com.threerings.everything.client.game.BuyCoinsPage::onPurchaseComplete(Ljava/lang/String;)(
                data['status']);
        });
    }-*/;

    protected static final GameMessages _msgs = GWT.create(GameMessages.class);
    protected static final BlingImages _images = GWT.create(BlingImages.class);
}
