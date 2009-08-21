//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FX;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.SlotStatus;

import client.ui.ButtonUI;
import client.util.ClickCallback;
import client.util.Context;
import client.util.PopupCallback;

/**
 * Displays a full-sized card in a nice looking popup.
 */
public class CardPopup extends PopupPanel
{
    public static ClickHandler onClick (final Context ctx, final CardIdent ident,
                                        final Value<SlotStatus> status)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                final Widget centerOn = (Widget)event.getSource();
                _gamesvc.getCard(ident, new PopupCallback<Card>() {
                    public void onSuccess (Card card) {
                        display(ctx, new CardPopup(ctx, card, status), centerOn);
                    }
                });
            }
        };
    }

    public static void display (Context ctx, GameService.FlipResult result,
                                Value<SlotStatus> status, Widget centerOn)
    {
        display(ctx, new CardPopup(ctx, result, status), centerOn);
    }

    protected CardPopup (Context ctx, Card card, Value<SlotStatus> status)
    {
        this(ctx, status);
        setWidget(createContents(card));
    }

    protected CardPopup (Context ctx, GameService.FlipResult result, Value<SlotStatus> status)
    {
        this(ctx, status);
        _title = "You got the <b>" + result.card.thing.name + "</b> card!";
        _haveCount = result.haveCount;
        _thingsRemaining = result.thingsRemaining;
        setWidget(createContents(result.card));
    }

    protected CardPopup (Context ctx, Value<SlotStatus> status)
    {
        setStyleName("popup");
        addStyleName("card");
        _ctx = ctx;
        _status = status;
    }

    protected Widget createContents (final Card card)
    {
        // if we're looking at someone else's card, we don't need any fancy stuff
        if (!_ctx.getMe().equals(card.owner)) {
            PushButton want = ButtonUI.newButton("Want", ThingDialog.makeWantHandler(_ctx, card));
            want.setTitle("Post to your Facebook feed that you want this card.");
            return CardView.create(card, _title, null, want, ButtonUI.newButton("Close", onHide()));
        }

        String status = null;
        if (_haveCount > 1) {
            status = "You already have " + _haveCount + " of these cards.";
        } else if (_haveCount > 0) {
            status = "You already have this card.";
        } else if (_thingsRemaining == 1) {
            status = "You only need <b>one more card</b> to complete this series!";
        } else if (_thingsRemaining == 0) {
            status = "You have completed the <b>" + card.getSeries().name + "</b> series!";
        } else {
            int total = card.getSeries().things, have = (total - _thingsRemaining);
            status = "You have " + have + " of " + total + " " + card.getSeries().name + ".";
        }

        PushButton gift = ButtonUI.newButton(
            "Gift", GiftCardPopup.onClick(_ctx, card, new Runnable() {
            public void run () {
                onHide().onClick(null);
                _status.update(SlotStatus.GIFTED);
            }
        }, this));
        gift.setTitle("Give this card to a friend.");

        PushButton sell = ButtonUI.newButton("Sell");
        sell.setTitle("Sell this card back for half its value.");
        new ClickCallback<Integer>(sell) {
            protected boolean callService () {
                _gamesvc.sellCard(card.thing.thingId, card.received.getTime(), this);
                return true;
            }
            protected boolean gotResult (Integer result) {
                // let the client know we have an updated coins value
                _ctx.getCoins().update(result);
                onHide().onClick(null);
                _status.update(SlotStatus.SOLD);
                return false;
            }
        }.setConfirmHTML("You can sell the <b>" + card.thing.name + "</b> card for <b>" +
                         CoinLabel.getCoinHTML(card.thing.rarity.saleValue()) + "</b>. " +
                         "Do you want to sell it?");

        PushButton brag = ButtonUI.newButton("Brag", ThingDialog.makeGotHandler(_ctx, card));
        brag.setTitle("Post this card to your Facebook feed.");

        PushButton done = ButtonUI.newButton("Keep", onHide());
        done.setTitle("Keep this card for your collection.");

        return CardView.create(card, _title, status, sell, gift, brag, done);
    }

    protected ClickHandler onHide ()
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                int tx = -getOffsetWidth(), ty = getAbsoluteTop();
                FX.move(CardPopup.this).to(tx, ty).onComplete(new Command() {
                    public void execute () {
                        hide();
                    }
                }).run(500);
            }
        };
    }

    protected static void display (Context ctx, CardPopup popup, Widget centerOn)
    {
        popup.setVisible(false);
        ctx.displayPopup(popup, centerOn);
        FX.move(popup).from(-popup.getOffsetWidth(), popup.getAbsoluteTop()).run(500);
    }

    protected Context _ctx;
    protected String _title;
    protected int _haveCount, _thingsRemaining = -1;
    protected Value<SlotStatus> _status;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
