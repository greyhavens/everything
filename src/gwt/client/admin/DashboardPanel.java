//
// $Id$

package client.admin;

import java.util.Date;

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
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.AdminService;
import com.threerings.everything.client.AdminServiceAsync;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.News;
import com.threerings.everything.data.PlayerDetails;
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
        super("dashboard", ctx);
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
        contents.setWidget(0, 0, stats);
        contents.getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_TOP);

        // display our pending categories
        FlowPanel cats = new FlowPanel();
        cats.add(Widgets.newLabel("Pending Categories", "Header"));
        for (Category cat : data.pendingCategories) {
            cats.add(Args.createLink(cat.name, Page.EDIT_SERIES, cat.categoryId));
        }
        contents.setWidget(0, 1, cats);
        contents.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
        contents.getFlexCellFormatter().setRowSpan(0, 1, 2);

        // this is where we'll stuff player details
        SimplePanel details = new SimplePanel();

        // display our find player interface
        final TextBox search = Widgets.newTextBox("", 128, 20);
        DefaultTextListener.configure(search, "<find player>");
        final FlowPanel players = new FlowPanel();
        players.add(Widgets.newLabel("Find Player", "Header"));
        players.add(search);
        players.add(details);
        contents.setWidget(1, 0, players);
        contents.getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_TOP);

        // start out displaying recently joined players
        FlowPanel recent = new FlowPanel();
        for (PlayerName player : data.recentPlayers) {
            recent.add(Widgets.newActionLabel(
                           player.toString(), onPlayerClicked(details, player.userId)));
        }
        details.setWidget(recent);

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
            setText(0, 0, details.name.toString(), 2, "Header");
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

            final TextBox coins = Widgets.newTextBox("", 4, 4);
            final Button grant = new Button("Grant");
            row = addText("Grant coins:", 1, "Label");
            setWidget(row, 1, Widgets.newRow(coins, grant));
            new ClickCallback<Void>(grant, coins) {
                protected boolean callService () {
                    int togrant = Integer.parseInt(coins.getText().trim());
                    if (togrant <= 0) {
                        return false;
                    }
                    _adminsvc.grantCoins(details.name.userId, togrant, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    Popups.infoNear("Coins granted!", grant);
                    return true;
                }
            };
        }

        protected void addDatum (String name, Object value) {
            int row = addText(name + ":", 1, "Label");
            setText(row, 1, String.valueOf(value));
        }
    }

    protected Value<News> _news;

    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);
    protected static final DateTimeFormat _dfmt = DateTimeFormat.getMediumDateTimeFormat();
    protected static final DateTimeFormat _bfmt = DateTimeFormat.getMediumDateFormat();
}
