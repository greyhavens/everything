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
import com.threerings.everything.data.CardIdent;

import client.ui.DataPopup;
import client.util.Context;
import client.util.PopupCallback;

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

    public CardPopup (Context ctx, final Card card, int haveCount, Value<String> status)
    {
        this(ctx, status);
        _title = "You got the <b>" + card.thing.name + "</b> card!";
        _haveCount = haveCount;
        _doneLabel = "Keep"; // we're viewing a just flipped card
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
        if (_title != null) {
            contents.add(Widgets.newHTML(_title, "Title"));
        }
        contents.add(CardView.create(card));

        String haveMsg = (_haveCount > 1) ? "You already have " + _haveCount + " of these cards." :
            ((_haveCount > 0) ? "You already have this card." : null);
        if (haveMsg != null) {
            contents.add(Widgets.newLabel(haveMsg, null));
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
        if (card.owner.userId == _ctx.getMe().userId) {
            contents.add(Widgets.newRow("Buttons", sell, gift, done));
        } else {
            contents.add(Widgets.newRow("Buttons", done));
        }
        return contents;
    }

    protected String _title, _doneLabel;
    protected int _haveCount;
    protected Value<String> _status;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
