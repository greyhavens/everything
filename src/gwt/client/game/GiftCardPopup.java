//
// $Id$

package client.game;

import java.util.Collections;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EscapeClickAdapter;
import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.FriendCardInfo;

import client.ui.ButtonUI;
import client.ui.DataPopup;
import client.ui.XFBML;
import client.util.ClickCallback;
import client.util.Context;

/**
 * Displays information on which of a player's friends has a particular card and allows them to
 * gift the card to a friend.
 */
public class GiftCardPopup extends DataPopup<GameService.GiftInfoResult>
{
    public static ClickHandler onClick (final Context ctx, final Card card, final Runnable onGifted,
                                        final Widget trigger)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                ctx.displayPopup(new GiftCardPopup(ctx, card, onGifted), trigger);
            }
        };
    }

    public GiftCardPopup (Context ctx, Card card, final Runnable onGifted)
    {
        super("giftCard", ctx);
        _card = card;
        _onGifted = new Runnable() {
            public void run () {
                hide();
                onGifted.run();
            }
        };
        _gamesvc.getGiftCardInfo(card.thing.thingId, card.received.getTime(), createCallback());
    }

    @Override // from DataPopup<GameService.GiftInfoResult>
    protected Widget createContents (GameService.GiftInfoResult result)
    {
        Collections.sort(result.friends);

        FluentTable grid = new FluentTable(5, 0);
        int row = 0, col = 0;
        for (final FriendCardInfo info : result.friends) {
            String text = info.friend.toString();
            if (info.onWishlist) {
                text += " (on wishlist)";
            } else if (info.hasThings > 0) {
                text += " (has " + info.hasThings + "/" + result.things + ")";
            }
            grid.at(row, col).setText(text, "nowrap");
            grid.at(row, col+1).setWidget(ButtonUI.newSmallButton("Give", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _ctx.displayPopup(makeGiftPopup(info), GiftCardPopup.this);
                }
            }));
            col += 2;
            if (col > 3) {
                row++;
                col = 0;
            }
        }
        if (row > 0) {
            grid.setWidth("100%");
        }
        String msg = (result.friends.size() == 0) ?
            "All of your Everything friends already have this card." :
            "Friends that already have this card are not shown.";
        grid.add().setText(msg).setColSpan(3);

        FluentTable facebook = new FluentTable(5, 0);
        facebook.at(0, 0).setText("More Everything players = more fun!");
        facebook.at(0, 1).setWidget(ButtonUI.newSmallButton("Pick", new ClickHandler() {
            public void onClick (ClickEvent event) {
                _ctx.displayPopup(new InvitePopup(_ctx, _card, new Runnable() {
                    public void run () {
                        Popups.info("Card sent! Thanks for sharing the Everything love.");
                        _onGifted.run();
                    }
                }), GiftCardPopup.this);
            }
        }));

        return Widgets.newFlowPanel(
            Widgets.newLabel("Send " + _card.thing.name + " to a Facebook friend:", "machine"),
            facebook,
            Widgets.newShim(10, 10),
            Widgets.newLabel("Send " + _card.thing.name + " to an Everything friend:", "machine"),
            Widgets.newScrollPanelY(grid, 190),
            Widgets.newFlowPanel("Buttons", ButtonUI.newButton("Cancel", onHide())));
    }

    protected PopupPanel makeGiftPopup (final FriendCardInfo info)
    {
        final TextBox message = Widgets.newTextBox("", 255, 40);
        final CheckBox post = new CheckBox("Also post this to your Facebook feed");
        FluentTable table = new FluentTable(5, 0) {
            protected void onLoad () {
                super.onLoad();
                XFBML.parse(this);
                DeferredCommand.addCommand(new Command() {
                    public void execute () {
                        message.setFocus(true);
                    }
                });
            }
        };
        PopupPanel popup = Popups.newPopup("popup", table);
        table.add().setWidget(XFBML.newProfilePic(info.friend.facebookId)).
            right().setHTML("Give " + _card.thing.name + "<br>to " + info.friend, "machine");
        table.add().setText("Enter an optional message:").setColSpan(2);
        table.add().setWidget(message).setColSpan(2);
        post.setChecked(true);
        table.add().setWidget(post).setColSpan(2);
        final ClickHandler hider = Popups.createHider(popup);
        message.addKeyPressHandler(new EscapeClickAdapter(hider));
        final PushButton cancel = ButtonUI.newSmallButton("Cancel", hider);
        final PushButton give = ButtonUI.newSmallButton("Send It");
        new ClickCallback<Void>(give, message) {
            protected boolean callService () {
                String msg = message.getText().trim();
                _gamesvc.giftCard(
                    _card.thing.thingId, _card.received.getTime(), info.friend.userId, msg, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                if (!post.getValue()) {
                    Popups.info("Card gifted. Your friend will be so happy!");
                }
                hider.onClick(null); // hide ourselves
                _onGifted.run();
                if (post.getValue()) {
                    ThingDialog.showGifted(_ctx, _card, info.friend);
                }
                return false;
            }
        };
        table.add().setWidget(Widgets.newRow(give, Widgets.newShim(25, 5), cancel)).
            setColSpan(2).alignCenter();
        return popup;
    }

    protected Card _card;
    protected long _received;
    protected Runnable _onGifted;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
