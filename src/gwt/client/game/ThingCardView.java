//
// $Id$

package client.game;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.data.ThingCard;

import client.util.ImageUtil;

/**
 * Displays a small version of the front of a card.
 */
public class ThingCardView extends FlowPanel
{
    public static Widget create (int pos, ThingCard card, Command onClick)
    {
        return new ThingCardView(card, onClick);
    }

    protected ThingCardView (ThingCard card, Command onClick)
    {
        setStyleName("thingCard");
        boolean back = (card == null || card.image == null);
        addStyleName(back ? "thingCardBack" : "thingCardFront");

        String name = (card == null || card.name == null) ? "?" : card.name;
        add(Widgets.newLabel(name, "Name", back ? "handwriting" : "machine"));
        add(ImageUtil.getMiniImageBox(card == null ? null : card.image, onClick));
        add(new RarityLabel(card == null ? null : card.rarity));
    }
}
