//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.PushButton;

import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.ValueLabel;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Powerup;

import client.ui.DataPanel;
import client.ui.PowerupUI;
import client.util.ClickCallback;
import client.util.Context;
import client.util.Messages;

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

        SmartTable table = new SmartTable(5, 0);
        table.setWidget(0, 0, Widgets.newRow(Widgets.newLabel("You have:", "machine"),
                                             new CoinLabel(_ctx.getCoins())), 5);

        table.setText(1, 0, "Powerup", 2, "Header");
        table.setText(1, 1, "Have", 1, "Header");
        table.setText(1, 2, "Cost", 2, "Header", "center");

        for (final Powerup type : Powerup.values()) {
            int row = table.addWidget(PowerupUI.newIcon(type), 1, "Icon");
            table.setWidget(row, 1, Widgets.newFlowPanel(
                                Widgets.newLabel(Messages.xlate(type.toString()), "Name"),
                                Widgets.newLabel(Messages.xlate(type + "_descrip"),
                                                 "handwriting")), 1);
            table.setWidget(row, 2, ValueLabel.create(_ctx.getPupsModel().getCharges(type)),
                            1, "right");
            table.setWidget(row, 3, new CoinLabel(type.cost), 1, "right");
            if (type.charges > 1) {
                table.setText(row, 4, "for " + type.charges, 1, "nowrap");
            }
            final PushButton buy = new PushButton("Buy");
            table.setWidget(row, 5, buy);
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
                    _ctx.getCoins().update(_ctx.getCoins().get() - type.cost);
                    _ctx.getPupsModel().notePurchase(type);
                    Popups.infoNear("Powerup purchased!", buy);
                    return true;
                }
            };
        }
        add(table);
    }

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
