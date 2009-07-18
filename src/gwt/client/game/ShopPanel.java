//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;

import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.ValueLabel;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Powerup;

import client.ui.DataPanel;
import client.util.ClickCallback;
import client.util.Context;
import client.util.PowerupLookup;

/**
 * Displays the shop where a player can buy powerups.
 */
public class ShopPanel extends DataPanel<GameService.ShopResult>
{
    public ShopPanel (Context ctx)
    {
        super(ctx, "page", "shop");
        _gamesvc.getShopInfo(createCallback());
    }

    @Override // from DataPanel
    protected void init (GameService.ShopResult data)
    {
        _ctx.getPupsModel().refresh(data.powerups);

        add(Widgets.newLabel("Welcome to the Everything shop!", "Title"));

        SmartTable table = new SmartTable(5, 0);
        table.setWidget(0, 0, Widgets.newRow(Widgets.newLabel("You have:", null),
                                             new CoinLabel(_ctx.getCoins())));

        table.setText(1, 0, "Powerup", 1, "Header");
        table.setText(1, 1, "Have", 1, "Header");
        table.setText(1, 2, "Cost", 1, "Header");

        for (final Powerup type : Powerup.values()) {
            int row = table.addWidget(
                Widgets.newFlowPanel(
                    Widgets.newLabel(_pmsgs.xlate(type.toString()), "Name"),
                    Widgets.newLabel(_pmsgs.xlate(type + "_descrip"), "tipLabel")), 1, null);
            table.setWidget(row, 1, ValueLabel.create(_ctx.getPupsModel().getCharges(type)),
                            1, "right");
            table.setWidget(row, 2, new CoinLabel(type.cost), 1, "right");
            if (type.charges > 1) {
                table.setHTML(row, 3, "for " + type.charges);
            }
            final Button buy = new Button("Buy");
            table.setWidget(row, 4, buy);
            new ClickCallback<Void>(buy) {
                protected boolean callService () {
                    if (_ctx.getCoins().get() < type.cost) {
                        Popups.errorNear("You don't have enough coins to buy that.", buy);
                        return false;
                    }
                    _gamesvc.buyPowerup(type, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    _ctx.getPupsModel().notePurchase(type);
                    Popups.infoNear("Powerup purchased!", buy);
                    return true;
                }
            };
        }
        add(table);
    }

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
    protected static final PowerupLookup _pmsgs = GWT.create(PowerupLookup.class);
}
