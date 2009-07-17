//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Powerup;

import client.ui.DataPanel;
import client.util.Context;

/**
 * Displays the shop where a player can buy powerups.
 */
public class ShopPanel extends DataPanel<GameService.ShopResult>
{
    public ShopPanel (Context ctx)
    {
        super("shop", ctx);
        _gamesvc.getShopInfo(createCallback());
    }

    @Override // from DataPanel
    protected void init (GameService.ShopResult data)
    {
        SmartTable table = new SmartTable(5, 0);
        for (Powerup powerup : Powerup.values()) {
            Integer count = data.powerups.get(powerup);
            int row = table.addText(powerup.toString(), 1, null);
            table.setText(row, 1, (count == null) ? "0" : count.toString());
        }
        add(table);
    }

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
