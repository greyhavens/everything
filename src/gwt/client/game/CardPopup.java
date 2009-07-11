//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Card;

import client.util.ClickCallback;
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
                ctx.displayPopup(new CardPopup(ctx, ownerId, thingId, created));
            }
        };
    }

    public CardPopup (Context ctx, int ownerId, int thingId, long created)
    {
        this(ctx);
        setWidget(Widgets.newLabel("Loading...", "infoLabel"));
        _gamesvc.getCard(ownerId, thingId, created, new PopupCallback<Card>() {
            public void onSuccess (Card card) {
                init(card);
            }
        });
    }

    public CardPopup (Context ctx, final Card card)
    {
        this(ctx);
        init(card);
    }

    protected CardPopup (Context ctx)
    {
        setStyleName("card");
        _ctx = ctx;
    }

    protected void init (final Card card)
    {
        final FlowPanel contents = new FlowPanel();
        contents.add(new CardView.Front(card));
        Button flip = new Button("Flip", new ClickHandler() {
            public void onClick (ClickEvent event) {
                Widget face = contents.getWidget(0);
                contents.remove(0);
                if (face instanceof CardView.Front) {
                    contents.insert(new CardView.Back(card), 0);
                } else {
                    contents.insert(new CardView.Front(card), 0);
                }
            }
        });
        Button sell = new Button("Sell");
        new ClickCallback<Integer>(sell) {
            protected String getConfirmMessage () {
                return "You can sell the <b>" + card.thing.name + "</b> card for <b>" +
                    CoinLabel.getCoinHTML(card.thing.rarity.saleValue()) +
                    "</b>. Do you want to sell it?";
            }
            protected boolean confirmMessageIsHTML () {
                return true;
            }
            protected boolean callService () {
                _gamesvc.sellCard(card.thing.thingId, card.created.getTime(), this);
                return true;
            }
            protected boolean gotResult (Integer result) {
                // let the client know we have an updated coins value
                _ctx.getCoins().update(result);
                CardPopup.this.hide();
                Popups.info("Sold!");
                return false;
            }
        };
        Button done = new Button("Done", new ClickHandler() {
            public void onClick (ClickEvent event) {
                CardPopup.this.hide();
            }
        });
        contents.add(Widgets.newRow(flip, sell, done));
        setWidget(contents);
        center();
    }

    protected Context _ctx;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
