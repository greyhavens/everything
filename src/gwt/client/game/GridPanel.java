//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;

import com.samskivert.depot.util.ByteEnumUtil;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.ThingCard;

import client.util.Context;
import client.util.PanelCallback;
import client.util.PopupCallback;

/**
 * Displays a player's grid, allows flipping of cards.
 */
public class GridPanel extends FlowPanel
{
    public GridPanel (Context ctx)
    {
        setStyleName("grid");
        _ctx = ctx;

        add(Widgets.newLabel("Loading...", "infoLabel"));
        _gamesvc.getGrid(new PanelCallback<GameService.GridResult>(this) {
            public void onSuccess (GameService.GridResult data) {
                init(data);
            }
        });
    }

    protected void init (final GameService.GridResult data)
    {
        clear();

        add(_info = new SmartTable(5, 0));
        updateRemaining(data.grid.unflipped);
        _info.setText(0, 1, "New grid at " + _expfmt.format(data.grid.expires));

        add(_cards = new SmartTable(5, 0));
        for (int ii = 0; ii < data.grid.flipped.length; ii++) {
            int row = ii / COLUMNS, col = ii % COLUMNS;
            final int position = ii;
            ClickHandler onClick = (data.grid.flipped[ii] != null) ? null : new ClickHandler() {
                public void onClick (ClickEvent event) {
                    flipCard(data, position);
                }
            };
            _cards.setWidget(row, col, new ThingCardView(data.grid.flipped[ii], onClick));
        }

        add(_status = new SmartTable(5, 0));
        updateStatus(data.status);
        // TODO: _status.setText(0, 2, "Next free flip at " + _expfmt(data.nextFreeFlip));
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
        _info.setHTML(0, 0, "Remaining: " + buf, 1, "Bold");
    }

    protected void updateStatus (final GameStatus status)
    {
        // let the context know that we know of a fresher coins value
        _ctx.getCoins().update(status.coins);

        if (status.freeFlips > 0) {
            _status.setText(0, 0, "Free flips: " + status.freeFlips, 1, "Bold");
            _status.setText(0, 1, "");
        } else {
            _status.setWidget(0, 0, new CoinLabel("Next flip ", status.nextFlipCost), 1, "Bold");
            _status.setWidget(0, 1, new CoinLabel("You have ", _ctx.getCoins()));
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
                    _cards.setWidget(row, col, new ThingCardView(card, null));

                    // update our status
                    data.grid.unflipped[card.rarity.ordinal()]--;
                    updateRemaining(data.grid.unflipped);
                    updateStatus(data.status = result.status);

                    // display the card big and fancy and allow them to gift it or cash it in
                    Value<String> status = new Value<String>("");
                    _ctx.displayPopup(new CardPopup(_ctx, result.card, status));
                    status.addListener(new Value.Listener<String>() {
                        public void valueChanged (String status) {
                            _cards.setText(row, col, status);
                        }
                    });
                }
            });
    }

    protected Context _ctx;
    protected SmartTable _info, _cards, _status;

    protected static final DateTimeFormat _expfmt = DateTimeFormat.getFormat("H:MMa EEEEEEEEE");
    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);

    protected static final int COLUMNS = 4;
}
