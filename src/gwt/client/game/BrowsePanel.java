//
// $Id$

package client.game;

import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.ValueLabel;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.PlayerCollection;
import com.threerings.everything.data.SeriesCard;

import client.ui.DataPanel;
import client.ui.XFBML;
import client.util.Context;
import client.util.PanelCallback;

/**
 * Displays a player's collection.
 */
public class BrowsePanel extends DataPanel<PlayerCollection>
{
    public BrowsePanel (Context ctx, int ownerId)
    {
        super(ctx, "page", "browse");
        _gamesvc.getCollection(ownerId, createCallback());
    }

    @Override // from DataPanel
    protected void init (final PlayerCollection coll)
    {
        SmartTable header = new SmartTable("machine", 5, 0);
        header.setWidget(0, 0, XFBML.newProfilePic(coll.owner.facebookId));
        header.setText(0, 1, coll.owner.toString() + "'s Collection", 2, "Title");
        // header.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_BOTTOM);

        final SmartTable table = new SmartTable("handwriting", 5, 0);
        for (Map.Entry<String, Map<String, List<SeriesCard>>> cat : coll.series.entrySet()) {
            String catname = cat.getKey();
            for (Map.Entry<String, List<SeriesCard>> subcat : cat.getValue().entrySet()) {
                String subcatname = subcat.getKey();
                final int row = table.addText(catname, 1);
                catname = ""; // subsequent rows don't repeat the same category
                table.setText(row, 1, subcatname);
                FlowPanel cards = new FlowPanel();
                for (final SeriesCard card : subcat.getValue()) {
                    if (cards.getWidgetCount() > 0) {
                        cards.add(Widgets.newInlineLabel(" "));
                    }
                    final Value<Integer> owned = new Value<Integer>(card.owned);
                    cards.add(Widgets.newActionLabel(card.name, "Series", new ClickHandler() {
                        public void onClick (ClickEvent event) {
                            showSeries(table, coll, card.categoryId, owned, row+1);
                        }
                    }));
                    cards.add(new ValueLabel<Integer>("Series", owned) {
                        protected String getText (Integer owned) {
                            return " (" + owned + " of " + card.things + ")";
                        }
                    });
                }
                table.setWidget(row, 2, cards);
                table.getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_TOP);
                table.getFlexCellFormatter().setVerticalAlignment(row, 1, HasAlignment.ALIGN_TOP);
            }
        }

        clear();
        add(header);
        add(table);
        XFBML.parse(this);
    }

    protected void showSeries (SmartTable table, PlayerCollection coll, int categoryId,
                               Value<Integer> owned, int row)
    {
        if (_showingRow > 0) {
            table.removeRow(_showingRow);
        }
        table.insertRow(row);
        table.setWidget(row, 0, new SeriesPanel(_ctx, coll.owner.userId, categoryId, owned), 3);
        _showingRow = row;
    }

    protected int _showingRow;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
