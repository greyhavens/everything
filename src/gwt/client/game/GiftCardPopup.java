//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.DefaultTextListener;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.FriendCardInfo;

import client.ui.DataPopup;
import client.util.ClickCallback;
import client.util.Context;

/**
 * Displays information on which of a player's friends has a particular card and allows them to
 * gift the card to a friend.
 */
public class GiftCardPopup extends DataPopup<GameService.GiftInfoResult>
{
    public static ClickHandler onClick (final Context ctx, final Card card, final Runnable onGifted)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                ctx.displayPopup(new GiftCardPopup(ctx, card.thing.thingId, card.created.getTime(),
                                                   onGifted));
            }
        };
    }

    public GiftCardPopup (Context ctx, int thingId, long created, Runnable onGifted)
    {
        super("giftCard", ctx);
        _thingId = thingId;
        _created = created;
        _onGifted = onGifted;
        _gamesvc.getGiftCardInfo(thingId, created, createCallback());
    }

    @Override // from DataPopup<GameService.GiftInfoResult>
    protected Widget createContents (GameService.GiftInfoResult result)
    {
        SmartTable grid = new SmartTable(5, 0);
        grid.setWidth("100%");
        int row = 0, col = 0;
        for (final FriendCardInfo info : result.friends) {
            String text = info.friend.toString();
            if (info.onWishlist) {
                text += " (on wishlist)";
            } else if (info.hasThings > 0) {
                text += " (has " + info.hasThings + "/" + result.things + ")";
            }
            grid.setText(row, col, text, 1, "nowrap");
            final TextBox message = Widgets.newTextBox("", 255, 20);
            final String defmsg = "<optional gift message>";
            DefaultTextListener.configure(message, defmsg);
            message.setVisible(false);
            grid.setWidget(row, col+1, message);
            final Button give = new Button("Give");
            grid.setWidget(row, col+2, give);
            new ClickCallback<Void>(give, message) {
                protected boolean callService () {
                    if (!message.isVisible()) {
                        message.setVisible(true);
                        give.setText("Send Gift");
                        return false;
                    }
                    String msg = DefaultTextListener.getText(message, defmsg);
                    _gamesvc.giftCard(_thingId, _created, info.friend.userId, msg, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    GiftCardPopup.this.hide();
                    _onGifted.run();
                    Popups.info("Card gifted. Your friend will be so happy!");
                    return false;
                }
            };
            col += 3;
            if (col > 5) {
                row++;
                col = 0;
            }
        }
        String msg = (result.friends.size() == 0) ? "All of your friends already have this card." :
            "Friends that already have this card are not shown.";
        grid.addText(msg, 6, null);
        return Widgets.newFlowPanel(
            Widgets.newScrollPanelY(grid, 400),
            Widgets.newFlowPanel("Buttons", new Button("Never Mind", onHide())));
    }

    protected int _thingId;
    protected long _created;
    protected Runnable _onGifted;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
