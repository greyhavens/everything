//
// $Id$

package client.game;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.data.Card;

/**
 * Displays a full-sized card in a nice looking popup.
 */
public class CardPopup extends PopupPanel
{
    public CardPopup (final Card card)
    {
        // TODO: disable auto-dismiss

        final FlowPanel contents = new FlowPanel();
        setWidget(contents);
        contents.add(new CardView.Front(card));
        contents.add(Widgets.newActionLabel("<flip>", new ClickHandler() {
            public void onClick (ClickEvent event) {
                Widget face = contents.getWidget(0);
                contents.remove(0);
                if (face instanceof CardView.Front) {
                    contents.insert(new CardView.Back(card), 0);
                } else {
                    contents.insert(new CardView.Front(card), 0);
                }
            }
        }));
    }
}
