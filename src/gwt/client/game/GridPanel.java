//
// $Id$

package client.game;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.samskivert.depot.util.ByteEnumUtil;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.ThingCard;

import client.ui.DataPanel;
import client.util.Context;
import client.util.PanelCallback;
import client.util.PopupCallback;

/**
 * Displays a player's grid, allows flipping of cards.
 */
public class GridPanel extends DataPanel<GameService.GridResult>
{
    public GridPanel (Context ctx)
    {
        super(ctx, "page", "grid");
        _gamesvc.getGrid(createCallback());
    }

    @Override // from DataPanel
    protected void init (final GameService.GridResult data)
    {
        clear();

        add(_info = new SmartTable(5, 0));
        _info.setText(1, 0, "Unflipped cards:");
        _info.setText(1, 2, "New grid " + format(data.grid.expires), 1, "right");
        updateRemaining(data.grid.unflipped);
        updateStatus(data.status);

        add(_cards = new SmartTable(5, 0));
        for (int ii = 0; ii < data.grid.flipped.length; ii++) {
            int row = ii / COLUMNS, col = ii % COLUMNS;
            final int position = ii;
            ClickHandler onClick = (data.grid.flipped[ii] != null) ? null : new ClickHandler() {
                public void onClick (ClickEvent event) {
                    flipCard(data, position);
                }
            };
            _cards.setWidget(row, col, ThingCardView.createMicro(data.grid.flipped[ii], onClick));
        }
    }

    protected void updateRemaining (int[] unflipped)
    {
        StringBuilder buf = new StringBuilder();
        for (int ii = 0; ii < unflipped.length; ii++) {
            if (unflipped[ii] == 0) {
                continue;
            }
            buf.append((buf.length() > 0) ? "&nbsp;&nbsp;" : "");
            Rarity rarity = ByteEnumUtil.fromByte(Rarity.class, (byte)ii);
            buf.append(rarity).append("-").append(unflipped[ii]);
        }
        _info.setHTML(1, 1, buf.toString(), 1, "Bold");
    }

    protected void updateStatus (final GameStatus status)
    {
        // let the context know that we know of a fresher coins value
        _ctx.getCoins().update(status.coins);

        _info.setWidget(0, 0, new CoinLabel("You have ", _ctx.getCoins()), 1, "left");
        if (status.freeFlips > 0) {
            _info.setText(0, 1, "Next flip is free!", 1, "Bold");
            _info.setText(0, 2, "Free flips left: " + status.freeFlips, 1, "right");
        } else {
            _info.setWidget(0, 1, new CoinLabel("Next flip costs ", status.nextFlipCost),
                            1, "Bold");
            _info.setText(0, 2, "");
        }
    }

    protected void flipCard (final GameService.GridResult data, final int position)
    {
        // TODO: disable all click handlers
        _gamesvc.flipCard(data.grid.gridId, position, data.status.nextFlipCost,
                          new PopupCallback<GameService.FlipResult>() {
                public void onSuccess (GameService.FlipResult result) {
                    // convert the card to a thing card and display it in the grid
                    ThingCard card = new ThingCard();
                    card.thingId = result.card.thing.thingId;
                    card.name = result.card.thing.name;
                    card.image = result.card.thing.image;
                    card.rarity = result.card.thing.rarity;
                    final int row = position / COLUMNS, col = position % COLUMNS;
                    _cards.setWidget(row, col, ThingCardView.createMicro(card, null));

                    // update our status
                    data.grid.unflipped[card.rarity.ordinal()]--;
                    updateRemaining(data.grid.unflipped);
                    updateStatus(data.status = result.status);

                    // display the card big and fancy and allow them to gift it or cash it in
                    Value<String> status = new Value<String>("");
                    _ctx.displayPopup(new CardPopup(_ctx, result.card, result.haveCount, status));
                    status.addListener(new Value.Listener<String>() {
                        public void valueChanged (String status) {
                            _cards.setText(row, col, status);
                        }
                    });
                }
            });
    }

    protected static String format (Date date)
    {
        return DateUtil.formatDateTime(date).toLowerCase();
    }

    protected SmartTable _info, _cards, _status;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);

    protected static final int COLUMNS = 4;
}
