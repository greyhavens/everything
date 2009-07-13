//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.ClickCallback;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Card;

import client.ui.DataPopup;
import client.util.Context;
import client.util.PopupCallback;

/**
 * Displays a full-sized card in a nice looking popup.
 */
public class CardPopup extends DataPopup<Card>
{
    public static ClickHandler onClick (final Context ctx, final int ownerId, final int thingId,
                                        final long created, final Value<String> status)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                ctx.displayPopup(new CardPopup(ctx, ownerId, thingId, created, status));
            }
        };
    }

    public CardPopup (Context ctx, int ownerId, int thingId, long created, Value<String> status)
    {
        this(ctx, status);
        _gamesvc.getCard(ownerId, thingId, created, createCallback());
    }

    public CardPopup (Context ctx, final Card card, Value<String> status)
    {
        this(ctx, status);
        setWidget(createContents(card));
    }

    protected CardPopup (Context ctx, Value<String> status)
    {
        super("card", ctx);
        _status = status;
    }

    protected Widget createContents (final Card card)
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
        Button gift = new Button("Gift", GiftCardPopup.onClick(_ctx, card, new Runnable() {
            public void run () {
                CardPopup.this.hide();
                _status.update("Gifted!");
            }
        }));
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
                _status.update("Sold!");
                return false;
            }
        };
        Button done = new Button("Done", onHide());
        contents.add(Widgets.newRow(flip, sell, gift, done));
        return contents;
    }

    protected Value<String> _status;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
