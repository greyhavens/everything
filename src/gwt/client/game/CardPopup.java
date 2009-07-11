//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Card;

import client.util.Context;
import client.util.PopupCallback;

/**
 * Displays a full-sized card in a nice looking popup.
 */
public class CardPopup extends PopupPanel
{
    public static ClickHandler onClick (
        final Context ctx, final int ownerId, final int thingId, final long created)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                ctx.displayPopup(new CardPopup(ownerId, thingId, created));
            }
        };
    }

    public CardPopup (int ownerId, int thingId, long created)
    {
        this();
        setWidget(Widgets.newLabel("Loading...", "infoLabel"));
        _gamesvc.getCard(ownerId, thingId, created, new PopupCallback<Card>() {
            public void onSuccess (Card card) {
                init(card);
            }
        });
    }

    public CardPopup (final Card card)
    {
        this();
        init(card);
    }

    protected CardPopup ()
    {
        setStyleName("card");
    }

    protected void init (final Card card)
    {
        final FlowPanel contents = new FlowPanel();
        contents.add(new CardView.Front(card));
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.add(new Button("Flip", new ClickHandler() {
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
        buttons.add(Widgets.newShim(5, 5));
        buttons.add(new Button("Done", new ClickHandler() {
            public void onClick (ClickEvent event) {
                CardPopup.this.hide();
            }
        }));
        contents.add(buttons);
        setWidget(contents);
        center();
    }

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
