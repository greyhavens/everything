//
// $Id$

package client.game;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Functions;

import com.threerings.everything.data.ThingCard;

import client.util.Context;
import client.util.ImageUtil;

/**
 * Displays a small version of the front of a card.
 */
public class ThingCardView extends FlowPanel
{
    public ThingCardView (Context ctx, ThingCard card, Command onClick)
    {
        setStyleName("thingCard");
        boolean back = (card == null || card.image == null);
        addStyleName(back ? "thingCardBack" : "thingCardFront");

        String name = (card == null || card.name == null) ? "?" : card.name;
        String nameStyle = (name.length() > (back ? 12 : 15)) ? "LongName" : "NormalName";
        add(Widgets.newLabel(name, "Name", nameStyle, back ? "machine" : "handwriting"));
        add(ImageUtil.getMiniImageBox(card == null ? null : card.image, onClick,
                                      ctx.popupShowing().map(Functions.NOT)));
        add(new RarityLabel(card == null ? null : card.rarity));
    }
}
