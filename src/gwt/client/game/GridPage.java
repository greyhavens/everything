//
// $Id$

package client.game;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.samskivert.depot.util.ByteEnumUtil;

import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.ValueLabel;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.Functions;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Grid;
import com.threerings.everything.data.GridStatus;
import com.threerings.everything.data.Powerup;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.ThingCard;

import client.ui.ButtonUI;
import client.ui.DataPanel;
import client.ui.PowerupUI;
import client.util.Args;
import client.util.ClickCallback;
import client.util.Context;
import client.util.Messages;
import client.util.Page;
import client.util.PanelCallback;
import client.util.PopupCallback;

/**
 * Displays a player's grid, allows flipping of cards.
 */
public class GridPage extends DataPanel<GameService.GridResult>
{
    public GridPage (Context ctx)
    {
        super(ctx, "page", "grid");
        addStyleName("machine");

        // if we expect that we've already got a grid, request that
        if (ctx.getGridExpiry().get() > System.currentTimeMillis()) {
            _gamesvc.getGrid(Powerup.NOOP, true, createCallback());
        } else {
            figurePreGrid();
        }
    }

    @Override // from DataPanel
    protected void init (GameService.GridResult data)
    {
        if (data == null) { // oops, we thought we had a valid grid, but didn't, refigure
            figurePreGrid();
        } else {
            _data = data;
            _ctx.getGridExpiry().update(_data.grid.expires.getTime());
            showGrid();
        }
    }

    protected void figurePreGrid ()
    {
        if (_ctx.getPupsModel().havePreGrid()) {
            // if we have pre-grid powerups, then just show the use powerup display
            showPreGrid();

        } else if (_ctx.isNewbie()) {
            // otherwise, if we're a newbie just fetch the grid directly
            _gamesvc.getGrid(Powerup.NOOP, false, createCallback());

        } else {
            // we're a non-newbie, show the "Buy powerups or Get your grid" display
            showPreGrid();
        }
    }

    protected void showPreGrid ()
    {
        clear();

        SmartTable contents = new SmartTable("PreGrid", 5, 0);
        contents.setText(0, 0, "It's time for a new grid!", 3, "Title", "machine");
        contents.setText(1, 0, "Click a powerup:", 1, "machine", "center");
        contents.setWidget(2, 0, new PowerupsMenu(Powerup.PRE_GRID, null) {
            protected void activatePup (Label plabel, final Powerup pup,
                                        final Value<Integer> charges) {
                new ClickCallback<GameService.GridResult>(plabel) {
                    protected boolean callService () {
                        _gamesvc.getGrid(pup, false, this);
                        return true;
                    }
                    protected boolean gotResult (GameService.GridResult data) {
                        charges.update(charges.get()-1);
                        init(data);
                        return false;
                    }
                };
            }
        });

        contents.setText(1, 2, "Or get a stock grid:", 1, "machine");
        PushButton get = ButtonUI.newButton("Get!");
        new ClickCallback<GameService.GridResult>(get) {
            protected boolean callService () {
                _gamesvc.getGrid(Powerup.NOOP, false, this);
                return true;
            }
            protected boolean gotResult (GameService.GridResult data) {
                init(data);
                return false;
            }
        };
        contents.setWidget(2, 1, Widgets.newShim(50, 50));
        contents.setWidget(2, 2, get);
        contents.getFlexCellFormatter().setHorizontalAlignment(2, 2, HasAlignment.ALIGN_CENTER);
        contents.getFlexCellFormatter().setVerticalAlignment(2, 2, HasAlignment.ALIGN_TOP);
        add(contents);
    }

    protected void showGrid ()
    {
        clear();

        add(_info = new SmartTable("Info", 5, 0));
        Label pups = Widgets.newLabel("", "Powerups");
        Value<Boolean> enabled = _ctx.popupShowing().map(Functions.NOT);
        _info.setWidget(0, 2, hoverize(Widgets.makeActionable(pups, new ClickHandler() {
            public void onClick (ClickEvent event) {
                showPowerupsMenu((Widget)event.getSource());
            }
        }, enabled), enabled), 1, "PupBox");
        _info.getFlexCellFormatter().setRowSpan(0, 2, 2);
        _info.setText(1, 0, "Unflipped cards:");
        updateRemaining(_data.grid.unflipped);
        updateGameStatus(_data.status);

        add(_cards = new SmartTable("Cards", 5, 0));

        add(_status = new SmartTable("Status", 5, 0));
        _status.setText(0, 0, "New grid " + DateUtil.formatDateTime(_data.grid.expires));
        updateGrid();
    }

    protected void updateGrid ()
    {
        for (int ii = 0; ii < _data.grid.flipped.length; ii++) {
            int row = ii / COLUMNS, col = ii % COLUMNS;
            final int position = ii;
            ThingCard card = _data.grid.flipped[ii];
            ClickHandler onClick = (card != null && card.thingId > 0) ? null : new ClickHandler() {
                public void onClick (ClickEvent event) {
                    flipCard(position, (Widget)event.getSource());
                }
            };
            _cards.setWidget(row, col, new ThingCardView(_ctx, card, onClick));
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

        if (status.freeFlips > 0) {
            _info.setText(0, 0, "Free flips left: " + status.freeFlips, 1);
        } else if (status.nextFlipCost > 0) {
            _info.setWidget(0, 0, new CoinLabel("Next flip costs ", status.nextFlipCost), 1);
        } else {
            _info.setText(0, 0, "No more flips.", 1);
        }
        _info.setWidget(0, 1, new CoinLabel("You have ", _ctx.getCoins()), 1, "right");
    }

    protected void flipCard (final int position, final Widget trigger)
    {
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
                _cards.setWidget(row, col, new ThingCardView(_ctx, card, null));

                // update our status
                _data.grid.unflipped[card.rarity.ordinal()]--;
                updateRemaining(_data.grid.unflipped);
                updateGameStatus(_data.status = result.status);

                // display the card big and fancy and allow them to gift it or cash it in
                Value<String> status = new Value<String>("");
                status.addListener(new Value.Listener<String>() {
                    public void valueChanged (String status) {
                        _cards.setText(row, col, status);
                        _cards.getFlexCellFormatter().setHorizontalAlignment(
                            row, col, HasAlignment.ALIGN_CENTER);
                    }
                });
                _ctx.displayPopup(new CardPopup(_ctx, result, status), trigger);
            }
            public void onFailure (Throwable cause) {
                if (cause.getMessage().equals("e.nsf_for_flip")) {
                    _ctx.displayPopup(new NSFPopup(), trigger);
                } else {
                    super.onFailure(cause);
                }
            }
        });
    }

    protected void showPowerupsMenu (final Widget trigger)
    {
        final PopupPanel popup = new PopupPanel(true);
        popup.setStyleName("powerPopup");
        popup.setWidget(new PowerupsMenu(Powerup.POST_GRID, popup) {
            protected void activatePup (Label plabel, final Powerup pup,
                                        final Value<Integer> charges) {
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
        });
        Popups.showOver(popup, trigger);
    }

    protected class NSFPopup extends PopupPanel
    {
        public NSFPopup () {
            addStyleName("popup");
            addStyleName("nsfPopup");
            SmartTable table = new SmartTable(5, 0);
            table.setText(0, 0, "Oops, you're out of coins. What to do?", 2, "machine");
            table.setText(1, 0, "Wait 'til tomorrow and get more free flips.");
            table.setText(1, 1, "Get more coins now and keep flipping!");
            table.setWidget(2, 0, new PushButton("Wait", Popups.createHider(this)));
            table.setWidget(2, 1, new PushButton("Coins", Args.createLinkHandler(Page.GET_COINS)));
            setWidget(table);
        }
    }

    protected abstract class PowerupsMenu extends FlowPanel
    {
        public PowerupsMenu (Powerup[] pups, PopupPanel popup)
        {
            setStyleName("powerMenu");
            ClickHandler hider = (popup == null) ? null : Popups.createHider(popup);
            add(Widgets.newActionLabel("", "Top", hider));

            SmartTable items = new SmartTable("Items", 0, 0);
            add(items);
            for (final Powerup pup : pups) {
                final Value<Integer> charges = _ctx.getPupsModel().getCharges(pup);
                int row = items.addWidget(PowerupUI.newIcon(pup), 1, "Icon");
                items.getRowFormatter().setStyleName(row, "Item");
                Label plabel = Widgets.newInlineLabel(" " + Messages.xlate(pup.toString()));
                plabel.setTitle(Messages.xlate(pup + "_descrip"));
                if (charges.get() > 0) {
                    activatePup(plabel, pup, charges);
                } else {
                    items.getRowFormatter().addStyleName(row, "Disabled");
                }
                items.setWidget(row, 1, plabel);
                items.setWidget(row, 2, ValueLabel.create("inline", charges), 1, "Charges");
            }

            int row = items.addText("", 1);
            items.getRowFormatter().setStyleName(row, "Item");
            Hyperlink shop = Args.createLink("Buy Powerups", Page.SHOP);
            if (hider != null) {
                shop.addClickHandler(hider);
            }
            items.setWidget(row, 1, shop, 2);

            add(Widgets.newLabel("", "Bottom"));
        }

        protected abstract void activatePup (Label plabel, Powerup pup, Value<Integer> charges);
    }

    protected static Label hoverize (final Label target, Value<Boolean> enabled)
    {
        final MouseOverHandler onOver = new MouseOverHandler() {
            public void onMouseOver (MouseOverEvent event) {
                target.addStyleDependentName("up-hovering");
            }
        };
        final MouseOutHandler onOut = new MouseOutHandler() {
            public void onMouseOut (MouseOutEvent event) {
                target.removeStyleDependentName("up-hovering");
            }
        };
        enabled.addListenerAndTrigger(new Value.Listener<Boolean>() {
            public void valueChanged (Boolean enabled) {
                if (enabled && _over == null) {
                    _over = target.addMouseOverHandler(onOver);
                    _out = target.addMouseOutHandler(onOut);
                } else if (!enabled && _over != null) {
                    _over.removeHandler();
                    _out.removeHandler();
                    _over = _out = null;
                }
            }
            protected HandlerRegistration _over, _out;
        });
        return target;
    }

    protected GameService.GridResult _data;
    protected SmartTable _info, _cards, _status;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
    protected static final int COLUMNS = 4;
}
