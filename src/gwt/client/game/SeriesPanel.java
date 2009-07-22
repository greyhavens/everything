//
// $Id$

package client.game;

import java.util.HashSet;

import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Console;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.Series;
import com.threerings.everything.data.ThingCard;

import client.ui.DataPanel;
import client.util.Context;

/**
 * Displays thumbnails for all cards in a player's series.
 */
public class SeriesPanel extends DataPanel<Series>
{
    public SeriesPanel (Context ctx, int ownerId, int categoryId, Value<Integer> count)
    {
        super(ctx, "series");
        _ownerId = ownerId;
        _count = count;
        _gamesvc.getSeries(ownerId, categoryId, createCallback());
    }

    @Override // from DataPanel
    protected void init (final Series series)
    {
        final SmartTable grid = new SmartTable(5, 0);
        grid.addText(series.name, COLUMNS, "Title");
        for (int ii = 0; ii < series.things.length; ii++) {
            final int row = ii/COLUMNS+1, col = ii%COLUMNS;
            final ThingCard card = series.things[ii];
            Value<String> status = new Value<String>("");
            if (card != null) {
                Console.log("Wiring up status for " + card.thingId);
            }
            status.addListener(new Value.Listener<String>() {
                public void valueChanged (String status) {
                    grid.setText(row, col, status);
                    // this card was sold or gifted, so update our count
                    Set<Integer> ids = new HashSet<Integer>();
                    for (ThingCard tcard : series.things) {
                        if (tcard != null && tcard != card) {
                            ids.add(tcard.thingId);
                        }
                    }
                    Console.log("Updating count " + ids.size());
                    _count.update(ids.size());
                }
            });
            Command onClick = (card == null) ? null : CardPopup.onClick(
                _ctx, new CardIdent(_ownerId, card.thingId, card.created), status);
            grid.setWidget(row, col, ThingCardView.create(ii, card, onClick));
            grid.getFlexCellFormatter().setHorizontalAlignment(row, col, HasAlignment.ALIGN_CENTER);
            grid.getFlexCellFormatter().setVerticalAlignment(row, col, HasAlignment.ALIGN_MIDDLE);
        }
        add(grid);
    }

    protected int _ownerId;
    protected Value<Integer> _count;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
    protected static final int COLUMNS = 4;
}
