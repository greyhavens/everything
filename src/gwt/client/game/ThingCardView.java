//
// $Id$

package client.game;

import java.util.HashMap;

import java.util.Map;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Console;

import com.threerings.everything.data.ThingCard;

import client.ui.FlashBuilder;
import client.util.ImageUtil;

/**
 * Displays a small version of the front of a card.
 */
public class ThingCardView extends FlowPanel
{
    public static Widget create (int pos, ThingCard card, Command onClick)
    {
        boolean back = (card == null || card.image == null);
        FlashBuilder fb = new FlashBuilder("tc" + pos, back ?  "card_back" : "card_front");
        fb.addOr("title", (card == null) ? null : card.name, "?");
        fb.addIf("image", (card == null) ? null : card.image);
        fb.addOr("rarity", (card == null) ? null : card.rarity, "?");
        final String id = fb.id;
        SimplePanel cont = new SimplePanel() {
            protected void onUnload () {
                super.onUnload();
                _onClicks.remove(id);
            }
        };
        cont.setWidget(fb.build(140, 165, true));

        // wire up our static callback and map our click handler
        if (onClick != null) {
            configureCallback();
            _onClicks.put(fb.id, onClick);
        }

        return cont;
    }

    protected static void cardClicked (String id) {
        Command onClick = _onClicks.get(id);
        if (onClick == null) {
            Console.log("Got card click for which we have no handler", "id", id);
        } else {
            onClick.execute();
        }
    }

    protected static native void configureCallback () /*-{
        $wnd.cardClicked = function (id) {
           @client.game.ThingCardView::cardClicked(Ljava/lang/String;)(id);
        };
    }-*/;

    protected static Map<String, Command> _onClicks = new HashMap<String, Command>();

//     protected ThingCardView (ThingCard card, ClickHandler onClick)
//     {
//         setStyleName("thingCard");
//         String name = (card == null || card.name == null) ? "?" : card.name;
//         add(Widgets.newLabel(name, "Name"));
//         add(ImageUtil.getMiniImageBox(card == null ? null : card.image, onClick));
//         add(new RarityLabel(card == null ? null : card.rarity));
//     }
}
