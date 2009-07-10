//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Rarity;

import client.util.Context;
import client.util.PanelCallback;

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

    protected void init (GameService.GridResult data)
    {
        clear();
        StringBuilder buf = new StringBuilder();
        for (int ii = 0; ii < data.grid.unflipped.length; ii++) {
            if (data.grid.unflipped[ii] == 0) {
                continue;
            }
            buf.append((buf.length() > 0) ? "&nbsp;&nbsp;&nbsp;&nbsp;" : "");
            buf.append(Rarity.fromByte((byte)ii)).append("-").append(data.grid.unflipped[ii]);
        }
        add(new HTML("Remaining: " + buf, "Remaining"));

        SmartTable cards = new SmartTable(5, 0);
        for (int ii = 0; ii < data.grid.flipped.length; ii++) {
            int row = ii / COLUMNS, col = ii % COLUMNS;
            cards.setWidget(row, col, new ThingCardView(data.grid.flipped[ii]));
        }
        add(cards);

        if (data.status.freeFlips > 0) {
            add(Widgets.newLabel("Free flips: " + data.status.freeFlips, "Status"));
        } else {
            add(Widgets.newLabel("Next flip: " + data.status.nextFlipCost +
                                 " You have: " + data.status.coins, "Status"));
        }
    }

    protected Context _ctx;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);

    protected static final int COLUMNS = 4;
}
