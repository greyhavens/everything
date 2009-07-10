//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

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
        add(_remaining = Widgets.newHTML("", "Remaining"));
        updateRemaining(data.grid.unflipped);

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

        add(_status = Widgets.newHTML("", "Status"));
        updateStatus(data.status);
    }

    protected void updateRemaining (int[] unflipped)
    {
        StringBuilder buf = new StringBuilder();
        for (int ii = 0; ii < unflipped.length; ii++) {
            if (unflipped[ii] == 0) {
                continue;
            }
            buf.append((buf.length() > 0) ? "&nbsp;&nbsp;&nbsp;&nbsp;" : "");
            buf.append(Rarity.fromByte((byte)ii)).append("-").append(unflipped[ii]);
        }
        _remaining.setHTML("Remaining: " + buf);
    }

    protected void updateStatus (GameStatus status)
    {
        if (status.freeFlips > 0) {
            _status.setHTML("Free flips: " + status.freeFlips);
        } else {
            _status.setHTML("Next flip: " + status.nextFlipCost + " You have: " + status.coins);
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
                    _cards.setWidget(position / COLUMNS, position % COLUMNS,
                                     new ThingCardView(card, null));

                    // update our status
                    data.grid.unflipped[card.rarity.ordinal()]--;
                    updateRemaining(data.grid.unflipped);
                    updateStatus(data.status = result.status);

                    // TODO: display the card big and fancy and allow them to keep it, gift to a
                    // friend or cash it in
                }
            });
    }

    protected Context _ctx;
    protected SmartTable _cards;
    protected HTML _remaining, _status;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);

    protected static final int COLUMNS = 4;
}
