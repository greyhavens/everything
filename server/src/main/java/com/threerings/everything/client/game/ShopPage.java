//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.game;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.PushButton;

import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.ValueLabel;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.rpc.GameService;
import com.threerings.everything.rpc.GameServiceAsync;
import com.threerings.everything.data.Powerup;

import com.threerings.everything.client.ui.ButtonUI;
import com.threerings.everything.client.ui.DataPanel;
import com.threerings.everything.client.ui.PowerupUI;
import com.threerings.everything.client.util.Args;
import com.threerings.everything.client.util.ClickCallback;
import com.threerings.everything.client.util.Context;
import com.threerings.everything.client.util.Messages;
import com.threerings.everything.client.util.Page;

/**
 * Displays the shop where a player can buy powerups.
 */
public class ShopPage extends DataPanel<GameService.ShopResult>
{
    public ShopPage (Context ctx)
    {
        super(ctx, "page", "shop");
        _gamesvc.getShopInfo(createCallback());
    }

    @Override // from DataPanel
    protected void init (GameService.ShopResult data)
    {
        _ctx.getPupsModel().refresh(data.powerups);
        _ctx.getCoins().update(data.coins);

        FluentTable table = new FluentTable(5, 0);
        table.add().setColSpan(5).setWidget(
            Widgets.newRow("machine", Widgets.newLabel("You have:"),
                           new CoinLabel(_ctx.getCoins()),
                           Widgets.newShim(25, 5),
                           Args.createLink("Get Coins!", Page.GET_COINS)));

        table.add().setText("Powerup", "Header").setColSpan(2).
            right().setText("Have", "Header").
            right().setText("Cost", "Header", "center").setColSpan(2);

        Set<Powerup> preGrid = new HashSet<Powerup>(Arrays.asList(Powerup.PRE_GRID));
        for (final Powerup type : Powerup.values()) {
            if (type.cost <= 0) continue; // skip non-salable pups
            if (preGrid.contains(type)) continue; // we're phasing out pre-grid powerups

            final PushButton buy = ButtonUI.newButton("Buy");
            table.add().setWidget(PowerupUI.newIcon(type), "Icon").
                right().setWidgets(Widgets.newLabel(Messages.xlate(type.toString()), "Name"),
                                   Widgets.newLabel(Messages.xlate(type + "_descrip"),
                                                    "handwriting")).
                right().setWidget(ValueLabel.create(_ctx.getPupsModel().getCharges(type)), "right").
                right().setWidget(new CoinLabel(type.cost), "right").
                right().setText((type.charges > 1) ? ("for " + type.charges) : "", "nowrap").
                right().setWidget(buy);
            new ClickCallback<Void>(buy) {
                protected boolean callService () {
                    if (_ctx.getCoins().get() < type.cost) {
                        Popups.errorBelow("You don't have enough coins to buy that.", buy);
                        return false;
                    }
                    _gamesvc.buyPowerup(type, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    _ctx.getCoins().update(_ctx.getCoins().get() - type.cost);
                    _ctx.getPupsModel().notePurchase(type);
                    Popups.infoBelow("Powerup purchased!", buy);
                    if (type.isPermanent()) {
                        buy.setVisible(false);
                    }
                    return true;
                }
            };
            buy.setVisible(_ctx.getPupsModel().getCharges(type).get() == 0 || !type.isPermanent());
        }
        add(table);
    }

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
