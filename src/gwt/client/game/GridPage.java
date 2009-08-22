//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.samskivert.util.ByteEnumUtil;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.ValueLabel;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.Functions;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Grid;
import com.threerings.everything.data.Powerup;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.SlotStatus;
import com.threerings.everything.data.ThingCard;

import client.ui.ButtonUI;
import client.ui.DataPanel;
import client.ui.PowerupUI;
import client.util.Args;
import client.util.ClickCallback;
import client.util.Context;
import client.util.Messages;
import client.util.Page;
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

        PowerupsMenu menu = new PowerupsMenu(Powerup.PRE_GRID, null) {
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
        };

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

        FluentTable contents = new FluentTable(5, 0, "PreGrid");
        contents.add().setText("It's time for a new grid!", "Title", "machine").setColSpan(3);
        contents.add().setText("Click a powerup:", "machine", "center").
            right().right().setText("Or get a stock grid:", "machine");
        contents.add().setWidget(menu).
            right().setWidget(Widgets.newShim(50, 50)).
            right().setWidget(get).alignCenter().alignTop();
        add(contents);
    }

    protected void showGrid ()
    {
        clear();

        add(_info = new FluentTable(5, 0, "Info"));
        Label pups = Widgets.newLabel("", "Powerups");
        Value<Boolean> enabled = _ctx.popupShowing().map(Functions.NOT);
        _info.at(0, 2).setWidget(hoverize(Widgets.makeActionable(pups, new ClickHandler() {
            public void onClick (ClickEvent event) {
                showPowerupsMenu((Widget)event.getSource());
            }
        }, enabled), enabled), "PupBox").setRowSpan(2);
        _info.at(1, 0).setText("Unflipped cards:");
        updateRemaining(_data.grid.unflipped);
        updateGameStatus(_data.status);

        add(_cards = new FluentTable(5, 0, "Cards"));

        add(_status = new FluentTable(5, 0, "Status"));
        _status.at(0, 0).setText("New grid " + DateUtil.formatDateTime(_data.grid.expires));
        updateGrid();
    }

    protected void updateGrid ()
    {
        for (int ii = 0; ii < _data.grid.flipped.length; ii++) {
            int row = ii / COLUMNS, col = ii % COLUMNS;
            String text = getStatusText(_data.grid.slots[ii]);
            if (text != null) {
                _cards.at(row, col).setText(text, "Cell").alignCenter();
                continue;
            }

            final int position = ii;
            ThingCard card = _data.grid.flipped[ii];
            ClickHandler onClick;
            if (card != null && card.thingId > 0) {
                onClick = CardPopup.onClick(_ctx, new CardIdent(_ctx.getMe().userId, card.thingId,
                                                                card.received), createStatus(ii));
            } else {
                onClick = new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        flipCard(position, (Widget)event.getSource());
                    }
                };
            }
            _cards.at(row, col).setWidget(new ThingCardView(_ctx, card, onClick), "Cell");
        }
        _status.at(0, 1).setText("Grid status: " + Messages.xlate(""+_data.grid.status), "right");
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
        _info.at(1, 1).setHTML(buf.toString(), "Bold");
    }

    protected void updateGameStatus (GameStatus status)
    {
        // let the context know that we know of a fresher coins value
        _ctx.getCoins().update(status.coins);

        if (status.freeFlips > 0) {
            _info.at(0, 0).setText("Free flips left: " + status.freeFlips);
        } else if (status.nextFlipCost > 0) {
            _info.at(0, 0).setWidget(new CoinLabel("Next flip costs ", status.nextFlipCost));
        } else {
            _info.at(0, 0).setText("No more flips.");
        }
        _info.at(0, 1).setWidget(new CoinLabel("You have ", _ctx.getCoins()), "right");
    }

    protected void flipCard (final int position, final Widget trigger)
    {
        final FluentTable.Cell cell = _cards.at(position / COLUMNS, position % COLUMNS);
        _gamesvc.flipCard(_data.grid.gridId, position, _data.status.nextFlipCost,
                          new PopupCallback<GameService.FlipResult>() {
            public void onSuccess (GameService.FlipResult result) {
                // convert the card to a thing card and display it in the grid
                ThingCard card = new ThingCard();
                card.thingId = result.card.thing.thingId;
                card.name = result.card.thing.name;
                card.image = result.card.thing.image;
                card.rarity = result.card.thing.rarity;
                ThingCardView view = new ThingCardView(_ctx, card, CardPopup.onClick(
                    _ctx, result.card.getIdent(), createStatus(position)));
                cell.setWidget(view);

                // update our status
                _data.grid.slots[position] = SlotStatus.FLIPPED;
                _data.grid.unflipped[card.rarity.ordinal()]--;
                updateRemaining(_data.grid.unflipped);
                updateGameStatus(_data.status = result.status);

                // display the card big and fancy and allow them to gift it or cash it in
                CardPopup.display(_ctx, result, createStatus(position), view);
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

    protected Value<SlotStatus> createStatus (final int position)
    {
        Value<SlotStatus> status = new Value<SlotStatus>(_data.grid.slots[position]);
        status.addListener(new Value.Listener<SlotStatus>() {
            public void valueChanged (SlotStatus status) {
                _data.grid.slots[position] = status;
                _cards.at(position / COLUMNS, position % COLUMNS).
                    setText(getStatusText(status)).alignCenter();
            }
        });
        return status;
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

    protected static String getStatusText (SlotStatus status)
    {
        switch (status) {
        case GIFTED: return "Gifted!";
        case SOLD: return "Sold!";
        default: return null; // others not used
        }
    }

    protected class NSFPopup extends PopupPanel
    {
        public NSFPopup () {
            addStyleName("popup");
            addStyleName("nsfPopup");
            FluentTable table = new FluentTable(5, 0);
            table.add().setText("Oops, you're out of coins. What to do?", "machine").setColSpan(2);
            table.add().setText("Wait 'til tomorrow and get more free flips.").
                right().setText("Get more coins now and keep flipping!");
            table.add().setWidget(new PushButton("Wait", Popups.createHider(this))).
                right().setWidget(new PushButton("Coins", Args.createLinkHandler(Page.GET_COINS)));
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

            FluentTable items = new FluentTable(0, 0, "Items");
            add(items);
            for (final Powerup pup : pups) {
                Label plabel = Widgets.newInlineLabel(" " + Messages.xlate(pup.toString()));
                plabel.setTitle(Messages.xlate(pup + "_descrip"));
                final Value<Integer> charges = _ctx.getPupsModel().getCharges(pup);
                int row = items.add().setWidget(PowerupUI.newIcon(pup), "Icon").
                    right().setWidget(plabel).
                    right().setWidget(ValueLabel.create(charges, "inline"), "Charges").row;
                items.getRowFormatter().setStyleName(row, "Item");
                if (charges.get() > 0) {
                    activatePup(plabel, pup, charges);
                } else {
                    items.getRowFormatter().addStyleName(row, "Disabled");
                }
            }

            Hyperlink shop = Args.createLink("Buy Powerups", Page.SHOP);
            if (hider != null) {
                shop.addClickHandler(hider);
            }
            int row = items.add().setText("").right().setWidget(shop).setColSpan(2).row;
            items.getRowFormatter().setStyleName(row, "Item");

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
    protected FluentTable _info, _cards, _status;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
    protected static final int COLUMNS = 4;
}
