//
// $Id$

package client.game;

import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.PlayerCollection;
import com.threerings.everything.data.SeriesCard;

import client.util.Context;
import client.util.PanelCallback;

/**
 * Displays a player's collection.
 */
public class BrowsePanel extends FlowPanel
{
    public BrowsePanel (Context ctx, int ownerId)
    {
        setStyleName("browse");
        add(Widgets.newLabel("Loading...", "infoLabel"));
        _ctx = ctx;
        _gamesvc.getCollection(ownerId, new PanelCallback<PlayerCollection>(this) {
            public void onSuccess (PlayerCollection coll) {
                init(coll);
            }
        });
    }

    protected void init (PlayerCollection coll)
    {
        SmartTable table = new SmartTable(5, 0);
        table.addText(coll.owner.name + "'s Collection", 3, "Title");
        for (Map.Entry<String, Map<String, List<SeriesCard>>> cat : coll.series.entrySet()) {
            String catname = cat.getKey();
            for (Map.Entry<String, List<SeriesCard>> subcat : cat.getValue().entrySet()) {
                String subcatname = subcat.getKey();
                FlowPanel cards = new FlowPanel();
                for (SeriesCard card : subcat.getValue()) {
                    cards.add(Widgets.newActionLabel(card.name, "Series",
                                  SeriesPopup.onClick(_ctx, coll.owner.userId, card.categoryId)));
                    cards.add(Widgets.newLabel(" (" + card.owned + " of " + card.things + ")",
                                               "Series"));
                }
                int row = table.addText(catname, 1, null);
                table.setText(row, 1, subcatname);
                table.setWidget(row, 2, cards);
                table.getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_TOP);
                table.getFlexCellFormatter().setVerticalAlignment(row, 1, HasAlignment.ALIGN_TOP);
            }
        }
        clear();
        add(table);
    }

    protected Context _ctx;
    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
