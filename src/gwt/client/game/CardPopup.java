//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Build;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.Category;

import client.ui.ButtonUI;
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
    public static ClickHandler onClick (final Context ctx, final CardIdent ident,
                                        final Value<String> status)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                ctx.displayPopup(new CardPopup(ctx, ident, status), (Widget)event.getSource());
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
            PushButton want = ButtonUI.newButton("Want", makeWantHandler(card));
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
                CardPopup.this.hide();
                _status.update("Gifted!");
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
                CardPopup.this.hide();
                _status.update("Sold!");
                return false;
            }
        }.setConfirmHTML("You can sell the <b>" + card.thing.name + "</b> card for <b>" +
                         CoinLabel.getCoinHTML(card.thing.rarity.saleValue()) + "</b>. " +
                         "Do you want to sell it?");

        PushButton brag = ButtonUI.newButton("Brag", makeGotHandler(card));
        brag.setTitle("Post this card to your Facebook feed.");

        PushButton done = ButtonUI.newButton("Keep", onHide());
        done.setTitle("Keep this card for your collection.");

        return CardView.create(card, _title, status, sell, gift, brag, done);
    }

    protected ClickHandler makeGotHandler (Card card)
    {
        return makeThingHandler("got_card", Build.Template.GOT_CARD.id, card,
                                "Brag about your awesome card to your friends:", "Woo!");
    }

    protected ClickHandler makeWantHandler (Card card)
    {
        return makeThingHandler("want_card", Build.Template.WANT_CARD.id, card,
                                "Let your friends know you want this card:", "I wants it!");
    }

    protected ClickHandler makeThingHandler (String vec, final String templateId, final Card card,
                                             final String prompt, final String message)
    {
        final String cardURL = _ctx.getEverythingURL(
            vec, Page.BROWSE, card.owner.userId, card.thing.categoryId);
        final String everyURL = _ctx.getEverythingURL(vec, Page.LANDING);
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                showThingDialog(templateId, card.thing.name, card.thing.descrip,
                                Category.getHierarchy(card.categories),
                                card.thing.rarity.toString(),
                                ImageUtil.getImageURL(card.thing.image),
                                cardURL, everyURL, prompt, message);
            }
        };
    }

    protected static native void showThingDialog (String templateId, String thing, String descrip,
                                                  String category, String rarity, String image,
                                                  String cardURL, String everyURL,
                                                  String prompt, String message) /*-{
        var data = {
            'thing': thing,
            'descrip': descrip,
            'category': category,
            'rarity': rarity,
            'cardURL': cardURL,
            'everyURL': everyURL,
            'images': [{ 'src': image, 'href': cardURL }],
        };
        $wnd.FB.Connect.showFeedDialog(templateId, data, null, null, null, null, null,
                                       prompt, { value: message });
    }-*/;

// We used to use streamPublish, which is *way* simpler but does not allow us to highlight text in
// the title which is critical for making our feed stories not look like random news articles
//
//     protected static native void showThingDialog (String message, String thing, String descrip,
//                                                   String category, String rarity, String image,
//                                                   String url) /*-{
//         var attachment = {
//             'name': thing,
//             'href': url,
//             'description': descrip,
//             'media': [{ 'type': 'image',
//                         'src': image,
//                         'href': url }],
//             'properties': {'Category': category,
//                            'Rarity': rarity,
//                            'Join the fun': {'text': 'Play Everything!',
//                                             'href': 'http://apps.facebook.com/everythinggame/'}},
//         };
//         var actions = [{ "text": "Play Everything",
//                          "href": "http://apps.facebook.com/everythinggame/"}];
//         $wnd.FB.Connect.streamPublish(message, attachment, actions);
//     }-*/;

    protected String _title;
    protected int _haveCount, _thingsRemaining = -1;
    protected Value<String> _status;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
