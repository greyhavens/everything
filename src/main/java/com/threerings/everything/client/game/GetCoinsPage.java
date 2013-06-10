//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.ui.bling.BlingImages;
import com.threerings.everything.client.util.Args;
import com.threerings.everything.client.util.Context;
import com.threerings.everything.client.util.Page;

/**
 * Displays an landing page for buying coins which links to the OOO billing system. (Obsolete but
 * kept around for posterity.)
 */
public class GetCoinsPage extends FlowPanel
{
    public GetCoinsPage (Context ctx, String action)
    {
        addStyleName("getCoins");

        Widget frame = makeFrame(ctx, action);
        if (frame != null) {
            add(frame);
            return;
        }

        // display the main splash page
        addStyleName("page");
        add(Widgets.newLabel(_msgs.selectHeader(), "Header", "machine"));

        FluentTable prices = new FluentTable(5, 0, "Prices");
        int col = 0;
        for (int ii = 0; ii < PRICES.length; ii += 2) {
            Widget bucks = Widgets.newInlineLabel(" for $" + PRICES[ii], "machine");
            prices.at(0, col++).setWidgets(new CoinLabel(PRICES[ii+1]), bucks);
        }
        prices.at(0, col++).setText(YAYS[Random.nextInt(YAYS.length)], "handwriting");
        add(prices);

        FluentTable choices = new FluentTable(5, 0, "Methods");
        choices.add().setText( _msgs.selectSelect(), "machine").setColSpan(2);
        for (final Method method : METHODS) {
            PushButton button = Widgets.newPushButton(
                new Image(method.normal), new Image(method.hover), new Image(method.down),
                Args.createLinkHandler(Page.GET_COINS, method.target));
            button.setStyleName("BuyButton");
            choices.add().setWidget(button).right().setHTML(method.tip, "handwriting");
        }
        add(choices);
    }

    protected Widget makeFrame (Context ctx, String action)
    {
        String path;
        if (action.equals("admin")) {
            path = ctx.billingURL("admin/");
        } else if (action.equals("credit_card")) {
            path = ctx.billingURL("creditcard/choosecoins.jspx");
        } else if (action.equals("paypal")) {
            path = ctx.billingURL("paypal/choosecoins.jspx");
        } else if (action.equals("ooocard")) {
            path = ctx.billingURL("threeringscard/check.jspx?mode=coins");
        } else if (action.equals("boku")) {
            path = ctx.billingURL("boku/choosecoins.jspx");
        } else if (action.equals("paysafecard")) {
            path = ctx.billingURL("paysafecard/choosecoins.jspx");
        } else if (action.equals("other")) {
            path = ctx.billingURL("otheroptions.wm?choice=coins");
        } else {
            return null;
        }
        return new Frame(path);
    }

    protected static class Method
    {
        public ImageResource normal;
        public ImageResource hover;
        public ImageResource down;
        public String tip;
        public String target;

        public Method (ImageResource normal, ImageResource hover,
                       ImageResource down, String tip, String target) {
            this.normal = normal;
            this.hover = hover;
            this.down = down;
            this.tip = tip;
            this.target = target;
        }
    }

    protected static final GameMessages _msgs = GWT.create(GameMessages.class);
    protected static final BlingImages _images = GWT.create(BlingImages.class);

    protected static final int[] PRICES = { 5, 5000, 10, 11000, 20, 24000 };
    protected static final String[] YAYS = { _msgs.selectYay1(), _msgs.selectYay2(),
                                             _msgs.selectYay3(), _msgs.selectYay4() };

    protected static final Method[] METHODS = {
        new Method(_images.cc_default(), _images.cc_over(), _images.cc_down(),
                   _msgs.selectCCTip(), "credit_card"),
        new Method(_images.paypal_default(), _images.paypal_over(), _images.paypal_down(),
                   _msgs.selectPayPalTip(), "paypal"),
        new Method(_images.ooo_card_default(), _images.ooo_card_over(), _images.ooo_card_down(),
                   _msgs.selectOOOCardTip(), "ooocard"),
        new Method(_images.sms_default(), _images.sms_over(), _images.sms_down(),
                   _msgs.selectSMSTip(), "boku"),
//         new Method(_images.paysafe_default(), _images.paysafe_over(), _images.paysafe_down(),
//                    _msgs.selectPaysafeTip(), "paysafecard"),
//         new Method(_images.other_default(), _images.other_over(), _images.other_down(),
//                    _msgs.selectOtherTip(), "other"),
    };
}
