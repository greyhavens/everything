//
// $Id$

package client.game;

import java.util.HashSet;

import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;

import com.threerings.gwt.ui.FluentTable;
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
        final FluentTable grid = new FluentTable(5, 0);
        grid.add().setText(series.name, "Title").setColSpan(COLUMNS);
        for (int ii = 0; ii < series.things.length; ii++) {
            final FluentTable.Cell cell = grid.at(ii/COLUMNS+1, ii%COLUMNS);
            final ThingCard card = series.things[ii];
            Value<String> status = new Value<String>("");
            status.addListener(new Value.Listener<String>() {
                public void valueChanged (String status) {
                    cell.setText(status);
                    // this card was sold or gifted, so update our count
                    Set<Integer> ids = new HashSet<Integer>();
                    for (ThingCard tcard : series.things) {
                        if (tcard != null && tcard != card) {
                            ids.add(tcard.thingId);
                        }
                    }
                    _count.update(ids.size());
                }
            });
            ClickHandler onClick = (card == null) ? null : CardPopup.onClick(
                _ctx, new CardIdent(_ownerId, card.thingId, card.received), status);
            cell.setWidget(new ThingCardView(_ctx, card, onClick)).alignCenter().alignMiddle();
        }
        add(grid);
    }

    protected int _ownerId;
    protected Value<Integer> _count;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
    protected static final int COLUMNS = 5;
}
