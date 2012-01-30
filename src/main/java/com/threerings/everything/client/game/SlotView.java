//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.game;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.SimplePanel;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.SlotStatus;
import com.threerings.everything.data.ThingCard;

import com.threerings.everything.client.util.Context;

/**
 * Displays a card in a slot. Handles updating the displayed card or replacing it with "sold" or
 * "gifted" or whatnot.
 */
public class SlotView extends SimplePanel
{
    public final Value<SlotStatus> status = Value.create(SlotStatus.UNFLIPPED);

    public SlotView ()
    {
        setStyleName("slot");
        status.addListener(new Value.Listener<SlotStatus>() {
            public void valueChanged (SlotStatus status) {
                setStatus(status);
            }
        });
    }

    public boolean setStatus (SlotStatus status)
    {
        String text;
        switch (status) {
        case GIFTED: text = "Gifted!"; break;
        case SOLD: text = "Sold!"; break;
        case RECRUIT_GIFTED: text = "Come back tomorrow to send another Free Gift!"; break;
        default: return false; // others not used
        }
        setWidget(Widgets.newLabel(text, "SlotStatus"));
        return true;
    }

    /**
     * @param onClick if null, a default ClickHandler will be used.
     */
    public void setCard (
        Context ctx, ThingCard card, boolean isGift, ClickHandler onClick)
    {
        setCard(ctx, card, ctx.getMe().userId, isGift, onClick);
    }

    /**
     * @param onClick if null, a default ClickHandler will be used.
     */
    public void setCard (
        Context ctx, ThingCard card, int ownerId, boolean isGift, ClickHandler onClick)
    {
        if (onClick == null && card != null && card.thingId != 0) {
            CardIdent ident = new CardIdent(ownerId, card.thingId, card.received);
            onClick = CardPopup.onClick(ctx, ident, status);
        }
        setWidget(new ThingCardView(ctx, card, isGift, onClick));
    }
}
