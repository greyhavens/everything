//
// $Id$

package client.game;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;

import com.threerings.everything.client.EverythingServiceAsync;
import com.threerings.everything.client.EverythingService;
import com.threerings.everything.data.PlayerStats;

import client.ui.ButtonUI;
import client.ui.DataPanel;
import client.ui.XFBML;
import client.util.Args;
import client.util.Context;
import client.util.Page;

/**
 * Displays all of a player's friends and allows them to browse their collections.
 */
public class FriendsPage extends DataPanel<List<PlayerStats>>
{
    public FriendsPage (Context ctx)
    {
        super(ctx, "page", "friends");
    }

    public void setMode (String mode)
    {
        try {
            _mode = Enum.valueOf(Mode.class, mode);
        } catch (Exception e) {
            _mode = Mode.THINGS;
        }
        if (_friends == null) {
            _everysvc.getFriends(createCallback());
        } else {
            clear();
            init(_friends);
        }
    }

    @Override // from DataPanel
    protected void init (List<PlayerStats> friends)
    {
        _friends = friends;
        Collections.sort(friends, _mode.comp);

        FluentTable table = new FluentTable(5, 0, "handwriting");
        // table.setWidth("100%");

        Label label = Widgets.newLabel("Invite friends to play Everything with you:", "machine");
        PushButton invite = ButtonUI.newSmallButton("Invite", new ClickHandler() {
            public void onClick (ClickEvent event) {
                _ctx.displayPopup(new InvitePopup(_ctx, null, null), (Widget)event.getSource());
            }
        });
        table.add().setWidget(Widgets.newRow(label, invite)).setColSpan(COLUMNS).alignCenter();

        if (friends.size() == 0) {
            return;
        }

        table.add().setWidget(Args.createLink("Collector", Page.FRIENDS, Mode.NAME), "machine").
            right().setWidget(Args.createLink("Things", Page.FRIENDS, Mode.THINGS), "machine").
            right().setWidget(Args.createLink("Gifted", Page.FRIENDS, Mode.GIFTS), "machine").
            right().setWidget(Args.createLink("Series", Page.FRIENDS, Mode.SERIES), "machine").
            right().setWidget(Args.createLink("Completed", Page.FRIENDS, Mode.COMP), "machine").
            right().setWidget(Args.createLink("Last online", Page.FRIENDS, Mode.ONLINE), "machine");
        for (PlayerStats ps : friends) {
            table.add().setWidget(Args.createInlink(ps.name)).
                right().setText(ps.things, "right").
                right().setText(ps.gifts, "right").
                right().setText(ps.series, "right").
                right().setText(ps.completeSeries, "right").
                right().setText(DateUtil.formatDateTime(ps.lastSession));
        }

        add(table);

        XFBML.parse(this);
    }

    protected enum Mode {
        NAME(new Comparator<PlayerStats>() {
            public int compare (PlayerStats one, PlayerStats two) {
                return one.name.name.compareTo(two.name.name);
            }
        }),
        THINGS(new Comparator<PlayerStats>() {
            public int compare (PlayerStats one, PlayerStats two) {
                return two.things - one.things; // no danger of overflow
            }
        }),
        GIFTS(new Comparator<PlayerStats>() {
            public int compare (PlayerStats one, PlayerStats two) {
                return two.gifts - one.gifts; // no danger of overflow
            }
        }),
        SERIES(new Comparator<PlayerStats>() {
            public int compare (PlayerStats one, PlayerStats two) {
                return two.series - one.series; // no danger of overflow
            }
        }),
        COMP(new Comparator<PlayerStats>() {
            public int compare (PlayerStats one, PlayerStats two) {
                return two.completeSeries - one.completeSeries; // no danger of overflow
            }
        }),
        ONLINE(new Comparator<PlayerStats>() {
            public int compare (PlayerStats one, PlayerStats two) {
                return two.lastSession.compareTo(one.lastSession);
            }
        });

        public final Comparator<PlayerStats> comp;

        Mode (Comparator<PlayerStats> comp) {
            this.comp = comp;
        }
    };

    protected Mode _mode;
    protected List<PlayerStats> _friends;

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
    protected static final int COLUMNS = 5;
}
