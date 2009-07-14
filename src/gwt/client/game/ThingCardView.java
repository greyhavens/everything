//
// $Id$

package client.game;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.data.ThingCard;

import client.util.ImageUtil;

/**
 * Displays a small version of the front of a card.
 */
public class ThingCardView extends FlowPanel
{
    public static ThingCardView create (ThingCard card, ClickHandler onClick)
    {
        return new ThingCardView(card, onClick);
    }

    public static ThingCardView createMicro (ThingCard card, ClickHandler onClick)
    {
        ThingCardView view = new ThingCardView(card, onClick);
        view.addStyleName("microCard");
        return view;
    }

    protected ThingCardView (ThingCard card, ClickHandler onClick)
    {
        setStyleName("thingCard");
        String name = (card == null || card.name == null) ? "?" : card.name;
        add(Widgets.newLabel(name, "Name"));
        add(ImageUtil.getMiniImageBox(card == null ? null : card.image, onClick));
        add(new RarityLabel(card == null ? null : card.rarity));
    }
}
