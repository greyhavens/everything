//
// $Id$

package client.game;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.SlotStatus;
import com.threerings.everything.data.ThingCard;

import client.util.Context;

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
        default: return false; // others not used
        }
        setWidget(Widgets.newLabel(text, "SlotStatus"));
        return true;
    }

    public void setCard (Context ctx, ThingCard card, boolean isGift, ClickHandler onClick)
    {
        if (onClick == null && card != null && card.thingId != 0) {
            CardIdent ident = new CardIdent(ctx.getMe().userId, card.thingId, card.received);
            onClick = CardPopup.onClick(ctx, ident, status);
        }
        setWidget(new ThingCardView(ctx, card, isGift, onClick));
    }
}
