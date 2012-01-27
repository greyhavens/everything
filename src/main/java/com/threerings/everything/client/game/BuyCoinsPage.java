//
// $Id$

package com.threerings.everything.client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.everything.data.Build;
import com.threerings.everything.data.CoinPrices;
import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Console;

import com.threerings.everything.client.ui.bling.BlingImages;
import com.threerings.everything.client.util.Args;
import com.threerings.everything.client.util.Context;
import com.threerings.everything.client.util.Page;

/**
 * Displays an interface for buying coins via Facebook Credits.
 */
public class BuyCoinsPage extends FlowPanel
{
    public BuyCoinsPage (Context ctx, String action)
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
                _images.buywithfb_up().createImage(),
                _images.buywithfb_up().createImage(),
                _images.buywithfb_down().createImage(), new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        startPurchase(""+offer.id);
                    }
                });
            button.setStyleName("BuyButton");
            Widget credits = Widgets.newFlowPanel(
                Widgets.newImage(_images.fbcredit(), "Credits"),
                Widgets.newInlineLabel(""+offer.credits, "machine"));
            offers.add().setWidget(new CoinLabel(offer.coins)).
                right().setText(_msgs.buyFor()).
                right().setWidget(credits).
                right().setWidget(button);
        }
        add(offers);
    }

    protected void onPurchaseComplete (String offerId)
    {
        Console.log("Purchase complete '" + offerId + "'");
    }

    protected native void startPurchase (String offerId) /*-{
        var pay = {
          method: 'pay',
          order_info: offerId,
          purchase_type: 'item'
        };
        $wnd.FB.ui(pay, function(data) {
            this.@com.threerings.everything.client.game.BuyCoinsPage::onPurchaseComplete(Ljava/lang/String;)(
                data['order_id']);
        });
    }-*/;

    protected static final GameMessages _msgs = GWT.create(GameMessages.class);
    protected static final BlingImages _images = GWT.create(BlingImages.class);
}
