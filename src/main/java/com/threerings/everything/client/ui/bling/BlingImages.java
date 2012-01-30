//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.ui.bling;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Images used on the billing page.
 */
public interface BlingImages extends ImageBundle
{
    @Resource("cc_default.png")
    AbstractImagePrototype cc_default ();

    @Resource("cc_down.png")
    AbstractImagePrototype cc_down ();

    @Resource("cc_over.png")
    AbstractImagePrototype cc_over ();

    @Resource("ooo_card_default.png")
    AbstractImagePrototype ooo_card_default ();

    @Resource("ooo_card_down.png")
    AbstractImagePrototype ooo_card_down ();

    @Resource("ooo_card_over.png")
    AbstractImagePrototype ooo_card_over ();

    @Resource("other_default.png")
    AbstractImagePrototype other_default ();

    @Resource("other_down.png")
    AbstractImagePrototype other_down ();

    @Resource("other_over.png")
    AbstractImagePrototype other_over ();

    @Resource("paypal_default.png")
    AbstractImagePrototype paypal_default ();

    @Resource("paypal_down.png")
    AbstractImagePrototype paypal_down ();

    @Resource("paypal_over.png")
    AbstractImagePrototype paypal_over ();

    @Resource("paysafe_default.png")
    AbstractImagePrototype paysafe_default ();

    @Resource("paysafe_down.png")
    AbstractImagePrototype paysafe_down ();

    @Resource("paysafe_over.png")
    AbstractImagePrototype paysafe_over ();

    @Resource("sms_default.png")
    AbstractImagePrototype sms_default ();

    @Resource("sms_down.png")
    AbstractImagePrototype sms_down ();

    @Resource("sms_over.png")
    AbstractImagePrototype sms_over ();

    @Resource("fbcredit.png")
    AbstractImagePrototype fbcredit ();

    @Resource("buywithfb_up.png")
    AbstractImagePrototype buywithfb_up ();

    @Resource("buywithfb_down.png")
    AbstractImagePrototype buywithfb_down ();
}
