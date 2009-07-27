//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.CardIdent;

import client.ui.DataPopup;
import client.util.Args;
import client.util.ClickCallback;
import client.util.Context;
import client.util.ImageUtil;
import client.util.Page;

/**
 * Displays a full-sized card in a nice looking popup.
 */
public class CardPopup extends DataPopup<Card>
{
    public static Command onClick (final Context ctx, final CardIdent ident,
                                   final Value<String> status)
    {
        return new Command() {
            public void execute () {
                ctx.displayPopup(new CardPopup(ctx, ident, status));
            }
        };
    }

    public CardPopup (Context ctx, CardIdent ident, Value<String> status)
    {
        this(ctx, status);
        _gamesvc.getCard(ident, createCallback());
    }

    public CardPopup (Context ctx, GameService.FlipResult result, Value<String> status)
    {
        this(ctx, status);
        _title = "You got the <b>" + result.card.thing.name + "</b> card!";
        _haveCount = result.haveCount;
        _thingsRemaining = result.thingsRemaining;
        setWidget(createContents(result.card));
    }

    protected CardPopup (Context ctx, Value<String> status)
    {
        super("card", ctx);
        _status = status;
    }

    protected Widget createContents (final Card card)
    {
        // if we're looking at someone else's card, we don't need any fancy stuff
        if (!_ctx.getMe().equals(card.owner)) {
            return CardView.create(card, _title, null, new PushButton("Close", onHide()));
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

        PushButton gift = new PushButton("Gift", GiftCardPopup.onClick(_ctx, card, new Runnable() {
            public void run () {
                CardPopup.this.hide();
                _status.update("Gifted!");
            }
        }));
        gift.setTitle("Give this card to a friend.");

        PushButton sell = new PushButton("Sell");
        sell.setTitle("Sell this card back for half its value.");
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

        PushButton brag = new PushButton("Brag", new ClickHandler() {
            public void onClick (ClickEvent event) {
                showBragDialog(card.thing.name, card.thing.descrip,
                               ImageUtil.getImageURL(card.thing.image),
                               "http://apps.facebook.com/everythinggame/?token=" +
                               Args.createLinkToken(Page.BROWSE, card.owner.userId,
                                                    card.thing.categoryId));
            }
        });
        brag.setTitle("Post this card to your Facebook feed.");

        PushButton done = new PushButton("Keep", onHide());
        done.setTitle("Keep this card for your collection.");

        return CardView.create(card, _title, status, sell, gift, brag, done);
    }

    protected static native void showBragDialog (String thing, String descrip, String image,
                                                 String url) /*-{
        $wnd.FB_ShowBragDialog(thing, descrip, image, url);
    }-*/;

    protected String _title;
    protected int _haveCount, _thingsRemaining = -1;
    protected Value<String> _status;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
