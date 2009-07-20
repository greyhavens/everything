//
// $Id$

package client.admin;

import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.DefaultTextListener;
import com.threerings.gwt.ui.LimitedTextArea;
import com.threerings.gwt.ui.NumberTextBox;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.AdminService;
import com.threerings.everything.client.AdminServiceAsync;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.News;
import com.threerings.everything.data.Player;
import com.threerings.everything.data.PlayerName;

import client.ui.DataPanel;
import client.util.Args;
import client.util.ClickCallback;
import client.util.Context;
import client.util.Page;
import client.util.PanelCallback;
import client.util.PopupCallback;

/**
 * Displays an admin dashboard.
 */
public class DashboardPanel extends DataPanel<AdminService.DashboardResult>
{
    public DashboardPanel (Context ctx, Value<News> news)
    {
        super(ctx, "page", "dashboard");
        _news = news;
        _adminsvc.getDashboard(createCallback());
    }

    protected void init (final AdminService.DashboardResult data)
    {
        clear();
        SmartTable contents = new SmartTable(5, 0);
        add(contents);

        // update the latest news in case it has changed
        _news.update(data.latestNews);

        // set up our thing database stats
        FlowPanel stats = new FlowPanel();
        stats.add(Widgets.newLabel("ThingStats", "Header"));
        stats.add(Widgets.newLabel("Total things: " + data.stats.totalThings, null));
        stats.add(Widgets.newLabel("Total categories: " + data.stats.totalCategories, null));
        stats.add(Widgets.newLabel("Total players: " + data.stats.totalPlayers, null));
        stats.add(Widgets.newLabel("Total cards: " + data.stats.totalCards, null));

        final NumberTextBox flips = NumberTextBox.newIntBox(2, 2);
        final Button grant = new Button("Grant");
        stats.add(Widgets.newRow(Widgets.newLabel("Grant free flips:", null), flips, grant));
        new ClickCallback<Void>(grant, flips) {
            protected boolean callService () {
                int togrant = flips.getNumber().intValue();
                if (togrant <= 0) {
                    return false;
                }
                _adminsvc.grantFreeFlips(0, togrant, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                Popups.infoNear("Free flips granted to everyone!", grant);
                return true;
            }
        }.setConfirmText("Are you sure you want to grant free flips to every player in the " +
                         "entire game?");

        contents.setWidget(0, 0, stats);
        contents.getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_TOP);

        // display our pending categories
        FlowPanel cats = new FlowPanel();
        for (Category cat : data.pendingCategories) {
            if (cats.getWidgetCount() > 0) {
                cats.add(Widgets.newInlineLabel(", "));
            }
            cats.add(Args.createInlink(cat.name, Page.EDIT_SERIES, cat.categoryId));
        }
        contents.setWidget(0, 1, Widgets.newFlowPanel(
                               Widgets.newLabel("Pending Categories", "Header"), cats));
        contents.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
        contents.getFlexCellFormatter().setRowSpan(0, 1, 2);

        // this is where we'll stuff player details
        final SimplePanel details = Widgets.newSimplePanel(null, "Details");

        // display our find player interface
        final TextBox search = Widgets.newTextBox("", 128, 20);
        DefaultTextListener.configure(search, "<find player>");
        final FlowPanel players = new FlowPanel();
        players.add(Widgets.newLabel("Find Player", "Header"));
        players.add(search);
        players.add(details);
        contents.setWidget(1, 0, players);
        contents.getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_TOP);
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
                    details.setWidget(Widgets.newLabel("No match.", null));
                } else {
                    details.setWidget(makePlayerList(details, players));
                }
                return true;
            }
        };

        // start out displaying recently joined players
        details.setWidget(makePlayerList(details, data.recentPlayers));

        // display our news editing and posting interface
        Widget ontitle = Widgets.newLabel("Latest News", "Header");
        final LimitedTextArea onews = new LimitedTextArea(News.MAX_NEWS_LENGTH, 80, 5);
        final Button upnews = new Button("Update");
        new ClickCallback<Void>(upnews) {
            protected boolean callService () {
                data.latestNews.text = onews.getText().trim();
                _adminsvc.updateNews(
                    data.latestNews.reported.getTime(), data.latestNews.text, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                Popups.infoNear("News updated.", upnews);
                _news.update(data.latestNews);
                return true;
            }
        };
        contents.setWidget(2, 0, Widgets.newFlowPanel(ontitle, onews, upnews), 2, null);
        setLatestNews(onews, upnews, data.latestNews);

        Widget ntitle = Widgets.newLabel("Add News", "Header");
        final LimitedTextArea nnews = new LimitedTextArea(News.MAX_NEWS_LENGTH, 80, 5);
        final Button post = new Button("Post");
        new ClickCallback<Long>(post) {
            protected boolean callService () {
                String text = nnews.getText().trim();
                if (text.length() == 0) {
                    return false;
                }
                _adminsvc.addNews(text, this);
                return true;
            }
            protected boolean gotResult (Long reported) {
                News news = new News();
                news.reported = new Date(reported);
                news.reporter = _ctx.getMe();
                news.text = nnews.getText().trim();
                _news.update(news);
                data.latestNews = news;
                setLatestNews(onews, upnews, data.latestNews);
                nnews.setText("");
                Popups.infoNear("News posted.", post);
                return true;
            }
        };
        contents.setWidget(3, 0, Widgets.newFlowPanel(ntitle, nnews, post), 2, null);
    }

    protected void setLatestNews (LimitedTextArea text, Button action, News news)
    {
        if (news != null) {
            text.setText(news.text);
        }
        text.getTextArea().setEnabled(news != null);
        action.setEnabled(news != null);
    }

    protected FlowPanel makePlayerList (SimplePanel details, List<PlayerName> players)
    {
        FlowPanel list = new FlowPanel();
        for (PlayerName player : players) {
            list.add(Widgets.newActionLabel(""+player, onPlayerClicked(details, player.userId)));
        }
        return list;
    }

    protected ClickHandler onPlayerClicked (final SimplePanel details, final int userId)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                _adminsvc.getPlayerDetails(userId, new PopupCallback<Player>() {
                    public void onSuccess (Player deets) {
                        details.setWidget(new PlayerDetailsPanel(deets));
                    }
                });
            }
        };
    }

    protected static class PlayerDetailsPanel extends SmartTable
    {
        public PlayerDetailsPanel (final Player details) {
            super("Details", 2, 0);
            setText(0, 0, details.name.toString(), 2, "Header");
            addDatum("Joined", _dfmt.format(details.joined));
            addDatum("Last seen", _dfmt.format(details.lastSession));
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
            int row = addText("Is editor:", 1, "Label");
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
            int row = addText(name + ":", 1, "Label");
            setText(row, 1, String.valueOf(value));
        }

        protected void addGrant (final Player details, String label, final String onGranted,
                                 final Granter granter)
        {
            final NumberTextBox amount = NumberTextBox.newIntBox(4, 4);
            final Button grant = new Button("Grant");
            int row = addText(label, 1, "Label");
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

    protected Value<News> _news;

    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);
    protected static final DateTimeFormat _dfmt = DateTimeFormat.getMediumDateTimeFormat();
    protected static final DateTimeFormat _bfmt = DateTimeFormat.getMediumDateFormat();
}
