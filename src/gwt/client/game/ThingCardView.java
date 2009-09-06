//
// $Id$

package client.game;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;

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
    public ThingCardView (Context ctx, ThingCard card, boolean isGift, ClickHandler onClick)
    {
        Mode mode = isGift ? Mode.GIFT :
            ((card == null || card.image == null) ? Mode.BACK : Mode.FRONT);

        setStyleName("thingCard");
        addStyleName(mode.styleName);

        String name = (card == null || card.name == null) ? "?" : card.name;
        String nameStyle = (name.length() > (mode.back ? 12 : 15)) ? "LongName" : "NormalName";
        add(Widgets.newLabel(name, "Name", nameStyle, mode.back ? "machine" : "handwriting"));
        add(ImageUtil.getMiniImageBox(card == null ? null : card.image, onClick,
                                      ctx.popupShowing().map(Functions.NOT)));
        add(new RarityLabel(card == null ? null : card.rarity));
    }

    protected enum Mode {
        BACK("Back", true), FRONT("Front", false), GIFT("Gift", true);

        public final String styleName;
        public final boolean back;

        Mode (String style, boolean back) {
            this.styleName = "thingCard" + style;
            this.back = back;
        }
    };
}
