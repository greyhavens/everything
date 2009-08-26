//
// $Id$

package client.game;

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

/**
 * Displays all of a player's friends and allows them to browse their collections.
 */
public class FriendsPage extends DataPanel<List<PlayerStats>>
{
    public FriendsPage (Context ctx)
    {
        super(ctx, "page", "friends");
        _everysvc.getFriends(createCallback());
    }

    @Override // from DataPanel
    protected void init (List<PlayerStats> friends)
    {
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

        table.add().setText("Collector", "machine").right().setText("Things", "machine").
            right().setText("Series", "machine").
            right().setText("Completed", "machine").
            right().setText("Last online", "machine");
        for (PlayerStats ps : friends) {
            table.add().setWidget(Args.createInlink(ps.name)).
                right().setText(ps.things, "right").
                right().setText(ps.series, "right").
                right().setText(ps.completeSeries, "right").
                right().setText(DateUtil.formatDateTime(ps.lastSession));
        }

        add(table);
        XFBML.parse(this);
    }

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
    protected static final int COLUMNS = 5;
}
