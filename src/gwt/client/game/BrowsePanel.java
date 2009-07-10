//
// $Id$

package client.game;

import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

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
        add(Widgets.newLabel("Loading...", "infoLabel"));
        _gamesvc.getCollection(ownerId, new PanelCallback<PlayerCollection>(this) {
            public void onSuccess (PlayerCollection coll) {
                init(coll);
            }
        });
    }

    protected void init (PlayerCollection coll)
    {
        clear();
        add(Widgets.newLabel(coll.owner.name + "'s Collection", "Title"));
        SmartTable table = new SmartTable(5, 0);
        for (Map.Entry<String, Map<String, List<SeriesCard>>> cat : coll.series.entrySet()) {
            String catname = cat.getKey();
            for (Map.Entry<String, List<SeriesCard>> subcat : cat.getValue().entrySet()) {
                String subcatname = subcat.getKey();
                for (SeriesCard card : subcat.getValue()) {
                    int row = table.addText(catname, 1, null);
                    table.setText(row, 1, subcatname);
                    table.setText(row, 2, card.name);
                    table.setText(row, 3, card.owned + " of " + card.things);
                }
            }
        }
        add(table);
    }

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
