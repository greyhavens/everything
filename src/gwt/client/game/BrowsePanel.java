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
import com.google.gwt.user.client.ui.Widget;

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
import client.util.Args;
import client.util.Context;
import client.util.PanelCallback;

/**
 * Displays a player's collection.
 */
public class BrowsePanel extends DataPanel<PlayerCollection>
{
    public BrowsePanel (Context ctx, int ownerId, int selCatId)
    {
        super(ctx, "page", "browse");
        _selectedCatId = selCatId;
        _gamesvc.getCollection(ownerId, createCallback());
    }

    @Override // from DataPanel
    protected void init (final PlayerCollection coll)
    {
        SmartTable header = new SmartTable("machine", 5, 0);
        header.setWidget(0, 0, XFBML.newProfilePic(coll.owner.facebookId));
        Widget title = Widgets.newLabel(coll.owner.toString() + "'s Collection", "Title");
        if (!_ctx.getMe().equals(coll.owner)) {
            title = Widgets.newFlowPanel(title, Args.createMessageAnchor(coll.owner));
        }
        header.setWidget(0, 1, title, 2, "Header");

        ClickHandler toCall = null;
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
                    ClickHandler onClick = new ClickHandler() {
                        public void onClick (ClickEvent event) {
                            showSeries(table, coll, card.categoryId, owned, row+1);
                        }
                    };
                    if (_selectedCatId == card.categoryId) {
                        toCall = onClick;
                    }
                    Widget name = Widgets.newActionLabel(card.name, "inline", onClick);
                    if (card.owned == card.things) {
                        name.addStyleName("Complete");
                    }
                    cards.add(name);
                    cards.add(new ValueLabel<Integer>("Held", owned) {
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

        // if we have a category that should be selected, select it now
        if (toCall != null) {
            toCall.onClick(null);
        }
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

    protected int _selectedCatId;
    protected int _showingRow;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
