//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.FriendCardInfo;

import client.ui.DataPopup;
import client.util.Context;

/**
 * Displays information on which of a player's friends has a particular card and allows them to
 * gift the card to a friend.
 */
public class GiftCardPopup extends DataPopup<GameService.GiftInfoResult>
{
    public static ClickHandler onClick (final Context ctx, final Card card)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                ctx.displayPopup(
                    new GiftCardPopup(ctx, card.thing.thingId, card.created.getTime()));
            }
        };
    }

    public GiftCardPopup (Context ctx, int thingId, long created)
    {
        super("giftCard", ctx);
        _gamesvc.getGiftCardInfo(thingId, created, createCallback());
    }

    @Override // from DataPopup<GameService.GiftInfoResult>
    protected Widget createContents (GameService.GiftInfoResult result)
    {
        SmartTable grid = new SmartTable(5, 0);
        for (FriendCardInfo info : result.friends) {
            int row = grid.addText(info.friend.name, 1, null);
            if (info.hasThings > 0) {
                grid.setText(row, 1, "Has " + info.hasThings + "/" + result.things, 1, null);
            }
            if (info.onWishlist) {
                grid.setText(row, 2, "Wishlist!", 1, "Wishlist");
            }
            grid.setWidget(row, 3, new Button("Give"));
        }
        if (result.friends.size() == 0) {
            grid.addText("All of your friends already have this card.", 4, null);
        }
        int row = grid.addWidget(new Button("Done", onHide()), 4, null);
        grid.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_CENTER);
        return grid;
    }

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
