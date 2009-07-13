//
// $Id$

package client.game;

import java.util.HashSet;

import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Console;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Series;
import com.threerings.everything.data.ThingCard;

import client.util.Context;

/**
 * Displays thumbnails for all cards in a player's series.
 */
public class SeriesPopup extends PopupPanel
{
    public static ClickHandler onClick (final Context ctx, final int ownerId, final int categoryId,
                                        final Value<Integer> count)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                ctx.displayPopup(new SeriesPopup(ctx, ownerId, categoryId, count));
            }
        };
    }

    public SeriesPopup (Context ctx, final int ownerId, int categoryId, Value<Integer> count)
    {
        setStyleName("series");
        setWidget(Widgets.newLabel("Loading...", "infoLabel"));
        _ctx = ctx;
        _count = count;
        _gamesvc.getSeries(ownerId, categoryId, new AsyncCallback<Series>() {
            public void onSuccess (Series series) {
                init(ownerId, series);
            }
            public void onFailure (Throwable t) {
                setWidget(Widgets.newLabel(t.getMessage(), "errorLabel"));
            }
        });
    }

    protected void init (int ownerId, final Series series)
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
            ClickHandler onClick = (card == null) ? null :
                CardPopup.onClick(_ctx, ownerId, card.thingId, card.created, status);
            grid.setWidget(row, col, ThingCardView.create(card, onClick));
            grid.getFlexCellFormatter().setHorizontalAlignment(row, col, HasAlignment.ALIGN_CENTER);
            grid.getFlexCellFormatter().setVerticalAlignment(row, col, HasAlignment.ALIGN_MIDDLE);
        }
        int row = grid.addWidget(new Button("Close", new ClickHandler() {
            public void onClick (ClickEvent event) {
                SeriesPopup.this.hide();
            }
        }), COLUMNS, null);
        grid.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_CENTER);
        setWidget(grid);
        center();
    }

    protected Context _ctx;
    protected Value<Integer> _count;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
    protected static final int COLUMNS = 5;
}
