//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.NumberTextBox;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;

import com.threerings.everything.rpc.AdminService;
import com.threerings.everything.rpc.AdminServiceAsync;
import com.threerings.everything.data.Player;
import com.threerings.everything.data.PlayerName;

import com.threerings.everything.client.ui.DataPanel;
import com.threerings.everything.client.util.Args;
import com.threerings.everything.client.util.ClickCallback;
import com.threerings.everything.client.util.Context;
import com.threerings.everything.client.util.PopupCallback;

/**
 * An admin page for viewing and editing players.
 */
public class PlayersPage extends DataPanel<AdminService.RegiStatsResult>
{
    public PlayersPage (Context ctx)
    {
        super(ctx, "page", "players");
        _adminsvc.getRegiStats(createCallback());
    }

    protected void init (final AdminService.RegiStatsResult result)
    {
        final FluentTable contents = new FluentTable(5, 0);
        add(contents);

        // we'll display search results and player details here
        final FluentTable.Cell target = contents.at(1, 0).alignTop();

        // display our recent registration stats in the left hand column
        contents.at(0, 0).setRowSpan(2).alignTop().setWidgets(
            Widgets.newLabel("Recent Regs", "machine"), makeRegisTable(result.regcounts));

        // display our recent players in the next column
        contents.at(0, 1).setRowSpan(2).alignTop().setWidgets(
            Widgets.newLabel("Recent Players", "machine"), makePlayerList(target, result.players));

        // display our find player interface
        final TextBox search = Widgets.newTextBox("", 128, 20);
        Widgets.setPlaceholderText(search, "<find player>");
        contents.at(0, 2).alignTop().setWidgets(Widgets.newLabel("Find Player", "machine"), search);
        new ClickCallback<List<PlayerName>>(new Button("fake"), search) {
            protected boolean callService () {
                String query = search.getText().trim();
                if (query.length() == 0) {
                    return false;
                }
                _adminsvc.findPlayers(query, this);
                return true;
            }
            protected boolean gotResult (List<PlayerName> players) {
                if (players.size() == 0) {
                    target.setText("No match.");
                } else {
                    target.setWidget(makePlayerList(target, players));
                }
                return true;
            }
        };
    }

    protected FluentTable makeRegisTable (Map<Date, Integer> regicounts)
    {
        // the map is sorted from earliest to latest and we need to reverse that
        List<Map.Entry<Date, Integer>> entries = new ArrayList<Map.Entry<Date, Integer>>();
        entries.addAll(regicounts.entrySet());
        Collections.reverse(entries);

        FluentTable regis = new FluentTable(0, 0);
        regis.setWidth("100%");
        for (Map.Entry<Date, Integer> entry : entries) {
            regis.add().setText(_dfmt.format(entry.getKey())).
                right().setText(entry.getValue(), "right");
        }
        return regis;
    }

    protected FlowPanel makePlayerList (FluentTable.Cell cell, List<PlayerName> players)
    {
        FlowPanel list = new FlowPanel();
        for (PlayerName player : players) {
            list.add(Widgets.newActionLabel(""+player, onPlayerClicked(cell, player.userId)));
        }
        return list;
    }

    protected ClickHandler onPlayerClicked (final FluentTable.Cell cell, final int userId)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                _adminsvc.getPlayerDetails(userId, new PopupCallback<Player>() {
                    public void onSuccess (Player deets) {
                        cell.setWidget(new PlayerDetailsPanel(deets));
                    }
                });
            }
        };
    }

    protected static class PlayerDetailsPanel extends FluentTable
    {
        public PlayerDetailsPanel (final Player details) {
            super(2, 0, "Details");
            add().setWidget(Args.createInlink(details.name), "machine").setColSpan(2);
            addDatum("User ID", details.name.userId);
            addDatum("Joined", DateUtil.formatDate(details.joined));
            addDatum("Last seen", DateUtil.formatDateTime(details.lastSession));
            addDatum("Coins", details.coins);
            addDatum("Free flips", details.freeFlips);
            addDatum("Birthdate", (details.birthdate/100) + "/" + (details.birthdate%100));
            addDatum("Timezone", details.timezone);

            final CheckBox isEditor = new CheckBox();
            isEditor.setValue(details.isEditor);
            new ClickCallback<Void>(isEditor) {
                protected boolean callService () {
                    _adminsvc.updateIsEditor(details.name.userId, isEditor.getValue(), this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    Popups.infoBelow("Editor status updated.", isEditor);
                    return true;
                }
            };
            add().setText("Is editor:", "right").right().setWidget(isEditor);

            addGrant(details, "Grant coins:", "Coins granted!", new Granter() {
                public void callService (int userId, int amount, ClickCallback<Void> callback) {
                    _adminsvc.grantCoins(userId, amount, callback);
                }
            });
            addGrant(details, "Grant free flips:", "Free flips granted!", new Granter() {
                public void callService (int userId, int amount, ClickCallback<Void> callback) {
                    _adminsvc.grantFreeFlips(userId, amount, callback);
                }
            });
        }

        protected void addDatum (String name, Object value) {
            add().setText(name + ":", "right").right().setText(value);
        }

        protected void addGrant (final Player details, String label, final String onGranted,
                                 final Granter granter)
        {
            final NumberTextBox amount = NumberTextBox.newIntBox(4, 4);
            final Button grant = new Button("Grant");
            add().setText(label, "right").right().setWidget(Widgets.newRow(amount, grant));
            new ClickCallback<Void>(grant, amount) {
                protected boolean callService () {
                    int togrant = amount.getNumber().intValue();
                    if (togrant <= 0) {
                        return false;
                    }
                    granter.callService(details.name.userId, togrant, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    Popups.infoBelow(onGranted, grant);
                    return true;
                }
            };
        }
    }

    protected static interface Granter {
        public void callService (int userId, int amount, ClickCallback<Void> callback);
    }

    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);
    protected static final DateTimeFormat _bfmt = DateTimeFormat.getFormat(
        DateTimeFormat.PredefinedFormat.DATE_MEDIUM);
    protected static final DateTimeFormat _dfmt = DateTimeFormat.getFormat("MMM dd");
}
