//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.data.ThingCard;

import client.util.ImageUtil;

/**
 * Displays a small version of the front of a card.
 */
public class ThingCardView extends FlowPanel
{
    public ThingCardView (ThingCard card)
    {
        setStyleName("thingCard");
        String name = (card == null || card.name == null) ? "?" : card.name;
        add(Widgets.newLabel(name, "Name"));
        add(ImageUtil.getMiniImageBox(card == null ? null : card.image));
        add(Widgets.newLabel(card == null ? "?" : card.rarity.toString(), "Rarity"));
    }
}
