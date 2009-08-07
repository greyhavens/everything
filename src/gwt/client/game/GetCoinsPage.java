//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.everything.data.Build;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

import client.ui.bling.BlingImages;
import client.util.Args;
import client.util.Context;
import client.util.Page;

/**
 * Displays the billing website wrapped in an iframe;
 */
public class GetCoinsPage extends FlowPanel
{
    public GetCoinsPage (Context ctx, String action)
    {
        addStyleName("getCoins");

        Widget frame = makeFrame(action);
        if (frame != null) {
            add(frame);
            return;
        }

        // display the main splash page
        addStyleName("page");
        add(Widgets.newLabel(_msgs.selectHeader(), "Header", "machine"));

        SmartTable prices = new SmartTable("Prices", 5, 0);
        int col = 0;
        for (int ii = 0; ii < PRICES.length; ii += 2) {
            Widget bucks = Widgets.newInlineLabel(" for $" + PRICES[ii], "machine");
            prices.setWidget(0, col++, Widgets.newFlowPanel(new CoinLabel(PRICES[ii+1]), bucks));
        }
        prices.setText(0, col++, _msgs.selectYay(), 1, "handwriting");
        add(prices);

        SmartTable choices = new SmartTable("Methods", 5, 0);
        choices.setText(0, 0, _msgs.selectSelect(), 2, "machine");
        for (final Method method : METHODS) {
            int row = choices.addWidget(
                Widgets.newPushButton(method.normal.createImage(), method.hover.createImage(),
                                      method.down.createImage(),
                                      Args.createLinkHandler(Page.GET_COINS, method.target)), 1);
            choices.setHTML(row, 1, method.tip, 1, "handwriting");
        }
        add(choices);
    }

    protected Widget makeFrame (String action)
    {
        String path;
        if (action.equals("admin")) {
            path = Build.billingURL("admin/");
        } else if (action.equals("credit_card")) {
            path = Build.billingURL("buy_coins.wm");
        } else if (action.equals("paypal")) {
            path = Build.billingURL("paypal/choosecoins.jspx");
        } else if (action.equals("ooocard")) {
            path = Build.billingURL("threeringscard/check.jspx?mode=coins");
        } else if (action.equals("mobill")) {
            path = Build.billingURL("mobill/choosecoins.jspx");
        } else if (action.equals("paysafecard")) {
            path = Build.billingURL("paysafecard/choosecoins.jspx");
        } else if (action.equals("other")) {
            path = Build.billingURL("otheroptions.wm?choice=coins");
        } else {
            return null;
        }
        return new Frame(path);
    }

    protected static class Method
    {
        public AbstractImagePrototype normal;
        public AbstractImagePrototype hover;
        public AbstractImagePrototype down;
        public String tip;
        public String target;

        public Method (AbstractImagePrototype normal, AbstractImagePrototype hover,
                       AbstractImagePrototype down, String tip, String target) {
            this.normal = normal;
            this.hover = hover;
            this.down = down;
            this.tip = tip;
            this.target = target;
        }
    }

    protected static final int[] PRICES = { 5, 5000, 10, 11000, 20, 24000 };

    protected static final GameMessages _msgs = GWT.create(GameMessages.class);
    protected static final BlingImages _images = GWT.create(BlingImages.class);

    protected static final Method[] METHODS = {
        new Method(_images.cc_default(), _images.cc_over(), _images.cc_down(),
                   _msgs.selectCCTip(), "credit_card"),
        new Method(_images.paypal_default(), _images.paypal_over(), _images.paypal_down(),
                   _msgs.selectPayPalTip(), "paypal"),
        new Method(_images.ooo_card_default(), _images.ooo_card_over(), _images.ooo_card_down(),
                   _msgs.selectOOOCardTip(), "ooocard"),
//         new Method(_images.sms_default(), _images.sms_over(), _images.sms_down(),
//                    _msgs.selectSMSTip(), "mobill"),
//         new Method(_images.paysafe_default(), _images.paysafe_over(), _images.paysafe_down(),
//                    _msgs.selectPaysafeTip(), "paysafecard"),
//         new Method(_images.other_default(), _images.other_over(), _images.other_down(),
//                    _msgs.selectOtherTip(), "other"),
    };
}
