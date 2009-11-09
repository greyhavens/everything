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
import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.util.Handlers;
import com.threerings.gwt.util.StringUtil;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.SlotStatus;

import client.ui.ButtonUI;
import client.ui.XFBML;
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

    public static ClickHandler recruitGiftClick (final Context ctx, final Card card,
                                                 final Value<SlotStatus> status)
    {
        // TODO: clean this up, combine with above?
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                final Widget centerOn = (Widget)event.getSource();
                display(ctx, new CardPopup(ctx, card, status), centerOn);
            }
        };
    }

    public static void display (Context ctx, GameService.CardResult result,
                                Value<SlotStatus> status, Widget centerOn, final String message)
    {
        final CardPopup popup = new CardPopup(ctx, result, status);
        if (!StringUtil.isBlank(message)) {
            popup.setGiftInfo(result.card.giver, message);
        }
        display(ctx, popup, centerOn);
    }

    protected CardPopup (Context ctx, Card card, Value<SlotStatus> status)
    {
        this(ctx, status);
        setWidget(createContents(card));
    }

    protected CardPopup (Context ctx, GameService.CardResult result, Value<SlotStatus> status)
    {
        this(ctx, status);
        if (result.card.giver == null) {
            _title = "You got the <b>" + result.card.thing.name + "</b> card!";
        } else if (result.card.giver.userId == Card.BIRTHDAY_GIVER_ID) {
            _title = "Happy birthday!";
        } else {
            _title = "A gift from " + result.card.giver;
        }
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

    protected void setGiftInfo (PlayerName giver, String message)
    {
        final FluentTable table = new FluentTable(5, 0);
        table.at(0, 0).setWidget(XFBML.newProfilePic(giver.facebookId));
        table.at(0, 1).setText("'" + message + "'").alignTop();
        _onAnimComplete = new Command() {
            public void execute () {
                _msgPop = Popups.newPopup("popup", table);
                _msgPop.addStyleName("cardMessagePopup");
                _msgPop.setVisible(false);
                _msgPop.show();
                XFBML.parse(_msgPop);
                int left = (Window.getClientWidth() - _msgPop.getOffsetWidth())/2;
                int top = getAbsoluteTop();
                _msgPop.setPopupPosition(left, top + 2); // account for shadow
                _msgPop.setVisible(true);
                FX.move(CardPopup.this).to(getAbsoluteLeft(), top+_msgPop.getOffsetHeight()-1).
                    run(500);
                // FX.move(_msgPop).to(left, top - _msgPop.getOffsetHeight()).run(500);
            }
        };
    }

    protected Widget createContents (Card card)
    {
        // if we're looking at a recruitment gift, just have close and gift
        if (card.owner == null) {
            return createRecruitGiftContents(card);

        // if we're looking at someone else's card, we don't need any fancy stuff
        } else if (!_ctx.getMe().equals(card.owner)) {
            return createFriendCardContents(card);

        } else {
            return createOwnCardContents(card);
        }
    }

    protected Widget createRecruitGiftContents (final Card card)
    {
        _title = "Give the gift of Everything...";
        PushButton gift = ButtonUI.newButton("Send", new ClickHandler() {
            public void onClick (ClickEvent event) {
                _ctx.displayPopup(new InvitePopup(_ctx, card, new Runnable() {
                    public void run () {
                        onHide().onClick(null);
                        _status.update(SlotStatus.GIFTED);
                    }
                }), CardPopup.this);
            }
        });
        PushButton cancel = ButtonUI.newButton("Cancel", onHide());
        return CardView.create(card, _title, null, cancel, gift);
    }

    protected Widget createFriendCardContents (Card card)
    {
        PushButton want = ButtonUI.newButton("Want", ThingDialog.makeWantHandler(_ctx, card));
        want.setTitle("Post to your Facebook feed that you want this card.");
        return CardView.create(card, _title, null, want, ButtonUI.newButton("Close", onHide()));
    }

    protected Widget createOwnCardContents (final Card card)
    {
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

        PushButton gift = ButtonUI.newButton(
            "Gift", GiftCardPopup.onClick(_ctx, card, new Runnable() {
                public void run () {
                    onHide().onClick(null);
                    _status.update(SlotStatus.GIFTED);
                }
            }, this));
        gift.setTitle("Give this card to a friend.");

        PushButton keep = ButtonUI.newButton("Keep", onHide());
        keep.setTitle("Keep this card for your collection.");

        boolean completed = (_haveCount == 0 && _thingsRemaining == 0);
        boolean wasGift = (card.giver != null);
        PushButton share = ButtonUI.newButton(wasGift ? "Thank" : "Share",
            Handlers.chain(ThingDialog.makeGotHandler(_ctx, card, completed), onHide()));
        if (wasGift) {
            share.setTitle("Thank your friend for the awesome gift!");
        } else {
            share.setTitle("Tell your friends about this awesome card.");
        }

        // only "incentivize" sharing if you just completed a series or you just received a gift
        // (not when you are looking at a card gifted previously)
        return (completed || (wasGift && (_title != null))) ?
            CardView.create(card, _title, status, sell, gift, keep, share) :
            CardView.create(card, _title, status, sell, gift, share, keep);
    }

    protected ClickHandler onHide ()
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                if (_msgPop != null) {
                    _msgPop.hide();
                }
                int tx = -getOffsetWidth(), ty = getAbsoluteTop();
                FX.move(CardPopup.this).to(tx, ty).onComplete(new Command() {
                    public void execute () {
                        hide();
                    }
                }).run(500);
            }
        };
    }

    protected static void display (Context ctx, final CardPopup popup, Widget centerOn)
    {
        popup.setVisible(false);
        ctx.displayPopup(popup, centerOn);
        FX.move(popup).from(-popup.getOffsetWidth(), popup.getAbsoluteTop()).
            onComplete(FX.delay(popup._onAnimComplete, 500)).run(500);
    }

    protected Context _ctx;
    protected String _title;
    protected int _haveCount, _thingsRemaining = -1;
    protected Value<SlotStatus> _status;

    protected Command _onAnimComplete;
    protected PopupPanel _msgPop;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
