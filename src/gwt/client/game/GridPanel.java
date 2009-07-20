//
// $Id$

package client.game;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.samskivert.depot.util.ByteEnumUtil;

import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.ValueLabel;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Grid;
import com.threerings.everything.data.GridStatus;
import com.threerings.everything.data.Powerup;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.ThingCard;

import client.ui.DataPanel;
import client.util.ClickCallback;
import client.util.Context;
import client.util.Messages;
import client.util.PanelCallback;
import client.util.PopupCallback;

/**
 * Displays a player's grid, allows flipping of cards.
 */
public class GridPanel extends DataPanel<GameService.GridResult>
{
    public GridPanel (Context ctx)
    {
        super(ctx, "page", "grid");
        _gamesvc.getGrid(createCallback());
    }

    @Override // from DataPanel
    protected void init (GameService.GridResult data)
    {
        clear();
        _data = data;

        add(_info = new SmartTable(5, 0));
        _info.setText(1, 0, "Unflipped cards:");
        _info.setWidget(1, 2, new Button("Powerups", new ClickHandler() {
            public void onClick (ClickEvent event) {
                showPowerupsMenu((Widget)event.getSource());
            }
        }), 1, "right");
        updateRemaining(_data.grid.unflipped);
        updateGameStatus(_data.status);

        add(_cards = new SmartTable(5, 0));

        add(_status = new SmartTable(5, 0));
        _status.setText(0, 0, "New grid " + format(_data.grid.expires));
        updateGrid();
    }

    protected void updateGrid ()
    {
        for (int ii = 0; ii < _data.grid.flipped.length; ii++) {
            int row = ii / COLUMNS, col = ii % COLUMNS;
            final int position = ii;
            ClickHandler onClick = (_data.grid.flipped[ii] != null) ? null : new ClickHandler() {
                public void onClick (ClickEvent event) {
                    flipCard(position);
                }
            };
            _cards.setWidget(row, col, ThingCardView.createMicro(_data.grid.flipped[ii], onClick));
        }
        _status.setText(0, 1, "Grid status: " + Messages.xlate(""+_data.grid.status), 1, "right");
    }

    protected void updateRemaining (int[] unflipped)
    {
        StringBuilder buf = new StringBuilder();
        for (int ii = 0; ii < unflipped.length; ii++) {
            if (unflipped[ii] == 0) {
                continue;
            }
            buf.append((buf.length() > 0) ? "&nbsp;&nbsp;" : "");
            Rarity rarity = ByteEnumUtil.fromByte(Rarity.class, (byte)ii);
            buf.append(rarity).append("-").append(unflipped[ii]);
        }
        _info.setHTML(1, 1, buf.toString(), 1, "Bold");
    }

    protected void updateGameStatus (GameStatus status)
    {
        // let the context know that we know of a fresher coins value
        _ctx.getCoins().update(status.coins);

        _info.setWidget(0, 0, new CoinLabel("You have ", _ctx.getCoins()), 1, "left");
        if (status.freeFlips > 0) {
            _info.setText(0, 1, "Next flip is free!", 1, "Bold");
            _info.setText(0, 2, "Free flips left: " + status.freeFlips, 1, "right");
        } else {
            _info.setWidget(0, 1, new CoinLabel("Next flip costs ", status.nextFlipCost),
                            1, "Bold");
            _info.setText(0, 2, "");
        }
    }

    protected void flipCard (final int position)
    {
        // TODO: disable all click handlers
        _gamesvc.flipCard(_data.grid.gridId, position, _data.status.nextFlipCost,
                          new PopupCallback<GameService.FlipResult>() {
                public void onSuccess (GameService.FlipResult result) {
                    // convert the card to a thing card and display it in the grid
                    ThingCard card = new ThingCard();
                    card.thingId = result.card.thing.thingId;
                    card.name = result.card.thing.name;
                    card.image = result.card.thing.image;
                    card.rarity = result.card.thing.rarity;
                    final int row = position / COLUMNS, col = position % COLUMNS;
                    _cards.setWidget(row, col, ThingCardView.createMicro(card, null));

                    // update our status
                    _data.grid.unflipped[card.rarity.ordinal()]--;
                    updateRemaining(_data.grid.unflipped);
                    updateGameStatus(_data.status = result.status);

                    // display the card big and fancy and allow them to gift it or cash it in
                    Value<String> status = new Value<String>("");
                    _ctx.displayPopup(new CardPopup(_ctx, result.card, result.haveCount, status));
                    status.addListener(new Value.Listener<String>() {
                        public void valueChanged (String status) {
                            _cards.setText(row, col, status);
                        }
                    });
                }
            });
    }

    protected void showPowerupsMenu (final Widget trigger)
    {
        FlowPanel contents = new FlowPanel();
        final PopupPanel popup = Popups.newPopup("popup", contents);
        popup.setAutoHideEnabled(true);
        for (final Powerup pup : Powerup.POST_GRID) {
            final Value<Integer> charges = _ctx.getPupsModel().getCharges(pup);
            Label plabel = Widgets.newInlineLabel(" " + Messages.xlate(pup.toString()));
            if (charges.get() > 0) {
                new ClickCallback<Grid>(plabel) {
                    protected boolean callService () {
                        popup.hide();
                        if (pup.getTargetStatus() == _data.grid.status) {
                            Popups.errorNear("This powerup will have no effect.", trigger);
                            return false;
                        }
                        _gamesvc.usePowerup(_data.grid.gridId, pup, this);
                        return true;
                    }
                    protected boolean gotResult (Grid grid) {
                        _data.grid = grid;
                        charges.update(charges.get()-1);
                        updateGrid();
                        return false;
                    }
                };
            }
            contents.add(Widgets.newFlowPanel(ValueLabel.create("inline", charges), plabel));
        }
        Popups.showNear(popup, trigger);
    }

    protected static String format (Date date)
    {
        return DateUtil.formatDateTime(date).toLowerCase();
    }

    protected GameService.GridResult _data;
    protected SmartTable _info, _cards, _status;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
    protected static final int COLUMNS = 4;
}
