//
// $Id$

package client.admin;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.DefaultTextListener;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.ClickCallback;

import com.threerings.everything.client.AdminService;
import com.threerings.everything.client.AdminServiceAsync;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.PlayerDetails;
import com.threerings.everything.data.PlayerFullName;

import client.util.Args;
import client.util.Context;
import client.util.Page;
import client.util.PanelCallback;
import client.util.PopupCallback;

/**
 * Displays an admin dashboard.
 */
public class DashboardPanel extends FlowPanel
{
    public DashboardPanel (Context ctx)
    {
        setStyleName("dashboard");
        add(Widgets.newLabel("Loading...", "infoLabel"));

        _ctx = ctx;
        _adminsvc.getDashboard(new PanelCallback<AdminService.DashboardResult>(this) {
            public void onSuccess (AdminService.DashboardResult data) {
                init(data);
            }
        });
    }

    protected void init (AdminService.DashboardResult data)
    {
        clear();
        SmartTable contents = new SmartTable(5, 0);
        add(contents);

        // set up our thing database stats
        contents.setText(0, 0, "Thing Stats", 1, "Header");
        FlowPanel stats = new FlowPanel();
        stats.add(Widgets.newLabel("Total things: " + data.stats.totalThings, null));
        stats.add(Widgets.newLabel("Total categories: " + data.stats.totalCategories, null));
        stats.add(Widgets.newLabel("Total players: " + data.stats.totalPlayers, null));
        stats.add(Widgets.newLabel("Total cards: " + data.stats.totalCards, null));
        contents.setWidget(1, 0, stats);

        // this is where we'll stuff player details
        SimplePanel details = new SimplePanel();

        // display our pending categories
        contents.setText(2, 0, "Pending Categories", 1, "Header");
        FlowPanel cats = new FlowPanel();
        for (Category cat : data.pendingCategories) {
            cats.add(Args.createLink(cat.name, Page.EDIT_SERIES, cat.categoryId));
        }
        contents.setWidget(3, 0, cats);

        // display our find player interface
        contents.setText(0, 1, "Find Player", 1, "Header");
        final FlowPanel players = new FlowPanel();
        final TextBox search = Widgets.newTextBox("", 128, 20);
        DefaultTextListener.configure(search, "<find player>");
        players.add(search);
        players.add(details);
        contents.setWidget(1, 1, players);
        contents.getFlexCellFormatter().setRowSpan(1, 1, 3);
        contents.getFlexCellFormatter().setVerticalAlignment(1, 1, HasAlignment.ALIGN_TOP);

        // start out displaying recently joined players
        FlowPanel recent = new FlowPanel();
        for (PlayerFullName player : data.recentPlayers) {
            recent.add(Widgets.newActionLabel(player.name + " " + player.surname,
                                              onPlayerClicked(details, player.userId)));
        }
        details.setWidget(recent);
    }

    protected ClickHandler onPlayerClicked (final SimplePanel details, final int userId)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                _adminsvc.getPlayerDetails(userId, new PopupCallback<PlayerDetails>() {
                    public void onSuccess (PlayerDetails deets) {
                        details.setWidget(new PlayerDetailsPanel(deets));
                    }
                });
            }
        };
    }

    protected static class PlayerDetailsPanel extends SmartTable
    {
        public PlayerDetailsPanel (final PlayerDetails details) {
            super("Details", 2, 0);
            setText(0, 0, details.fullName.name + " " + details.fullName.surname, 2, "Header");
            addDatum("Joined", _dfmt.format(details.joined));
            addDatum("Last seen", _dfmt.format(details.lastSession));
            addDatum("Coins", details.coins);
            addDatum("Free flips", details.freeFlips);
            addDatum("Birthday", _bfmt.format(details.birthday));
            addDatum("Timezone", details.timezone);

            final CheckBox isEditor = new CheckBox();
            isEditor.setValue(details.isEditor);
            new ClickCallback<Void>(isEditor) {
                protected boolean callService () {
                    _adminsvc.updateIsEditor(details.fullName.userId, isEditor.getValue(), this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    Popups.infoNear("Editor status updated.", isEditor);
                    return true;
                }
            };
            int row = addText("Is editor:", 1, "Label");
            setWidget(row, 1, isEditor);

            // TODO: add granting coins
        }

        protected void addDatum (String name, Object value) {
            int row = addText(name + ":", 1, "Label");
            setText(row, 1, String.valueOf(value));
        }
    }

    protected Context _ctx;

    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);
    protected static final DateTimeFormat _dfmt = DateTimeFormat.getMediumDateTimeFormat();
    protected static final DateTimeFormat _bfmt = DateTimeFormat.getMediumDateFormat();
}
