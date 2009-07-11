//
// $Id$

package client.game;

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
    public static ClickHandler onClick (final Context ctx, final int ownerId, final int categoryId)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                ctx.displayPopup(new SeriesPopup(ctx, ownerId, categoryId));
            }
        };
    }

    public SeriesPopup (Context ctx, final int ownerId, int categoryId)
    {
        setStyleName("series");
        setWidget(Widgets.newLabel("Loading...", "infoLabel"));
        _ctx = ctx;
        _gamesvc.getSeries(ownerId, categoryId, new AsyncCallback<Series>() {
            public void onSuccess (Series series) {
                init(ownerId, series);
            }
            public void onFailure (Throwable t) {
                setWidget(Widgets.newLabel(t.getMessage(), "errorLabel"));
            }
        });
    }

    protected void init (int ownerId, Series series)
    {
        SmartTable grid = new SmartTable(5, 0);
        grid.addText(series.name, COLUMNS, "Title");
        for (int ii = 0; ii < series.things.length; ii++) {
            int row = ii/COLUMNS+1, col = ii%COLUMNS;
            ThingCard card = series.things[ii];
            ClickHandler onClick = (card == null) ? null :
                CardPopup.onClick(_ctx, ownerId, card.thingId, card.created);
            grid.setWidget(row, col, new ThingCardView(card, onClick));
        }
        int row = grid.addWidget(new Button("Done", new ClickHandler() {
            public void onClick (ClickEvent event) {
                SeriesPopup.this.hide();
            }
        }), COLUMNS, null);
        grid.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_CENTER);
        setWidget(grid);
        center();
    }

    protected Context _ctx;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
    protected static final int COLUMNS = 4;
}
