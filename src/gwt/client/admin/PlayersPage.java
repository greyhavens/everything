//
// $Id$

package client.admin;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.DefaultTextListener;
import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.NumberTextBox;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;

import com.threerings.everything.client.AdminService;
import com.threerings.everything.client.AdminServiceAsync;
import com.threerings.everything.data.Player;
import com.threerings.everything.data.PlayerName;

import client.ui.DataPanel;
import client.util.Args;
import client.util.ClickCallback;
import client.util.Context;
import client.util.PopupCallback;

/**
 * An admin page for viewing and editing players.
 */
public class PlayersPage extends DataPanel<List<PlayerName>>
{
    public PlayersPage (Context ctx)
    {
        super(ctx, "page", "players");
        _adminsvc.getRecentPlayers(createCallback());
    }

    protected void init (final List<PlayerName> recentPlayers)
    {
        final FluentTable contents = new FluentTable(5, 0);
        add(contents);

        // display our recent players in the left hand column
        contents.setWidgets(0, 0, Widgets.newLabel("Recent Players", "machine"),
                            makePlayerList(contents, recentPlayers)).setRowSpan(2).alignTop();

        // display our find player interface
        final TextBox search = Widgets.newTextBox("", 128, 20);
        DefaultTextListener.configure(search, "<find player>");
        contents.setWidgets(0, 1, Widgets.newLabel("Find Player", "machine"), search).alignTop();
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
                    contents.setText(1, 0, "No match.");
                } else {
                    contents.setWidget(1, 0, makePlayerList(contents, players));
                }
                return true;
            }
        };
    }

    protected FlowPanel makePlayerList (FluentTable contents, List<PlayerName> players)
    {
        FlowPanel list = new FlowPanel();
        for (PlayerName player : players) {
            list.add(Widgets.newActionLabel(""+player, onPlayerClicked(contents, player.userId)));
        }
        return list;
    }

    protected ClickHandler onPlayerClicked (final FluentTable contents, final int userId)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                _adminsvc.getPlayerDetails(userId, new PopupCallback<Player>() {
                    public void onSuccess (Player deets) {
                        contents.setWidget(1, 0, new PlayerDetailsPanel(deets));
                    }
                });
            }
        };
    }

    protected static class PlayerDetailsPanel extends FluentTable
    {
        public PlayerDetailsPanel (final Player details) {
            super(2, 0, "Details");
            addWidget(Args.createInlink(details.name), "machine").setColSpan(2);
            addDatum("User ID", details.name.userId);
            addDatum("Joined", DateUtil.formatDate(details.joined));
            addDatum("Last seen", DateUtil.formatDateTime(details.lastSession));
            addDatum("Coins", details.coins);
            addDatum("Free flips", details.freeFlips);
            if (details.birthday != null) {
                addDatum("Birthday", _bfmt.format(details.birthday));
            }
            addDatum("Timezone", details.timezone);

            final CheckBox isEditor = new CheckBox();
            isEditor.setValue(details.isEditor);
            new ClickCallback<Void>(isEditor) {
                protected boolean callService () {
                    _adminsvc.updateIsEditor(details.name.userId, isEditor.getValue(), this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    Popups.infoNear("Editor status updated.", isEditor);
                    return true;
                }
            };
            int row = addText("Is editor:", "right").row;
            setWidget(row, 1, isEditor);

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
            int row = addText(name + ":", "right").row;
            setText(row, 1, String.valueOf(value));
        }

        protected void addGrant (final Player details, String label, final String onGranted,
                                 final Granter granter)
        {
            final NumberTextBox amount = NumberTextBox.newIntBox(4, 4);
            final Button grant = new Button("Grant");
            int row = addText(label, "right").row;
            setWidget(row, 1, Widgets.newRow(amount, grant));
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
                    Popups.infoNear(onGranted, grant);
                    return true;
                }
            };
        }
    }

    protected static interface Granter {
        public void callService (int userId, int amount, ClickCallback<Void> callback);
    }

    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);
    protected static final DateTimeFormat _bfmt = DateTimeFormat.getMediumDateFormat();
}
