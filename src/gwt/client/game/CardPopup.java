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
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.Category;

import client.ui.DataPopup;
import client.util.ClickCallback;
import client.util.Context;

/**
 * Displays a full-sized card in a nice looking popup.
 */
public class CardPopup extends DataPopup<Card>
{
    public static ClickHandler onClick (final Context ctx, final CardIdent ident,
                                        final Value<String> status)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                ctx.displayPopup(new CardPopup(ctx, ident, status));
            }
        };
    }

    public CardPopup (Context ctx, CardIdent ident, Value<String> status)
    {
        this(ctx, status);
        _doneLabel = "Close"; // we're viewing something from our collection
        _gamesvc.getCard(ident, createCallback());
    }

    public CardPopup (Context ctx, GameService.FlipResult result, Value<String> status)
    {
        this(ctx, status);
        _title = "You got the <b>" + result.card.thing.name + "</b> card!";
        _haveCount = result.haveCount;
        _thingsRemaining = result.thingsRemaining;
        _doneLabel = "Keep"; // we're viewing a just flipped card
        setWidget(createContents(result.card));
    }

    protected CardPopup (Context ctx, Value<String> status)
    {
        super("card", ctx);
        _status = status;
    }

    protected Widget createContents (final Card card)
    {
        final FlowPanel contents = new FlowPanel();
        if (_title != null) {
            contents.add(Widgets.newHTML(_title, "Title"));
        }
        contents.add(CardView.create(card));

        String msg = null;
        if (_haveCount > 1) {
            msg = "You already have " + _haveCount + " of these cards.";
        } else if (_haveCount > 0) {
            msg = "You already have this card.";
        } else if (_thingsRemaining == 1) {
            msg = "You only need <b>one more card</b> to complete this series!";
        } else if (_thingsRemaining == 0) {
            Category series = card.categories[card.categories.length-1];
            msg = "You have completed the <b>" + series + "</b> series!";
        }
        if (msg != null) {
            contents.add(Widgets.newHTML(msg, "Info"));
        }

        Button gift = new Button("Gift", GiftCardPopup.onClick(_ctx, card, new Runnable() {
            public void run () {
                CardPopup.this.hide();
                _status.update("Gifted!");
            }
        }));

        Button sell = new Button("Sell");
        new ClickCallback<Integer>(sell) {
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
        }.setConfirmHTML("You can sell the <b>" + card.thing.name + "</b> card for <b>" +
                         CoinLabel.getCoinHTML(card.thing.rarity.saleValue()) + "</b>. " +
                         "Do you want to sell it?");

        Button done = new Button(_doneLabel, onHide());
        if (_ctx.getMe().equals(card.owner)) {
            contents.add(Widgets.newRow("Buttons", sell, gift, done));
        } else {
            contents.add(Widgets.newRow("Buttons", done));
        }
        return contents;
    }

    protected String _title, _doneLabel;
    protected int _haveCount, _thingsRemaining = -1;
    protected Value<String> _status;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
