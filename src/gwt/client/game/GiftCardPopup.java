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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.DefaultTextListener;
import com.threerings.gwt.ui.EscapeClickAdapter;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.FriendCardInfo;
import com.threerings.everything.data.Thing;

import client.ui.ButtonUI;
import client.ui.DataPopup;
import client.ui.XFBML;
import client.util.ClickCallback;
import client.util.Context;
import client.util.Page;

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

    public GiftCardPopup (Context ctx, Card card, Runnable onGifted)
    {
        super("giftCard", ctx);
        _card = card;
        _onGifted = onGifted;
        _gamesvc.getGiftCardInfo(card.thing.thingId, card.received.getTime(), createCallback());
    }

    @Override // from DataPopup<GameService.GiftInfoResult>
    protected Widget createContents (GameService.GiftInfoResult result)
    {
        Collections.sort(result.friends);

        SmartTable grid = new SmartTable(5, 0);
        int row = 0, col = 0;
        for (final FriendCardInfo info : result.friends) {
            String text = info.friend.toString();
            if (info.onWishlist) {
                text += " (on wishlist)";
            } else if (info.hasThings > 0) {
                text += " (has " + info.hasThings + "/" + result.things + ")";
            }
            grid.setText(row, col, text, 1, "nowrap");
            grid.setWidget(row, col+1, ButtonUI.newSmallButton("Give", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _ctx.displayPopup(makeGiftPopup(info), (Widget)event.getSource());
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
        grid.addText(msg, 6);

        SmartTable facebook = new SmartTable(5, 0);
        facebook.setText(0, 0, "More Everything players = more fun!");
        facebook.setWidget(0, 1, ButtonUI.newSmallButton("Pick", new ClickHandler() {
            public void onClick (ClickEvent event) {
                GiftCardPopup.this.hide();
                _ctx.displayPopup(new InvitePopup(_ctx, _card), (Widget)event.getSource());
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
        SmartTable table = new SmartTable(5, 0) {
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
        int row = table.addWidget(XFBML.newProfilePic(info.friend.facebookId), 1);
        table.setHTML(row, 1, "Give " + _card.thing.name + "<br>to " + info.friend, 1, "machine");
        table.addText("Enter an optional message:", 2);
        table.addWidget(message, 2);
        post.setChecked(true);
        table.addWidget(post, 2);
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
                GiftCardPopup.this.hide(); // then hide our parent
                _onGifted.run();
                if (post.getValue()) {
                    ThingDialog.showGifted(_ctx, _card, info.friend);
                }
                return false;
            }
        };
        row = table.addWidget(Widgets.newRow(give, Widgets.newShim(25, 5), cancel), 2);
        table.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_CENTER);
        return popup;
    }

    protected Card _card;
    protected long _received;
    protected Runnable _onGifted;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
