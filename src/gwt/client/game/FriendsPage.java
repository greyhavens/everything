//
// $Id$

package client.game;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;

import com.threerings.everything.client.EverythingServiceAsync;
import com.threerings.everything.client.EverythingService;
import com.threerings.everything.data.FriendStatus;

import client.ui.ButtonUI;
import client.ui.DataPanel;
import client.ui.XFBML;
import client.util.Args;
import client.util.Context;

/**
 * Displays all of a player's friends and allows them to browse their collections.
 */
public class FriendsPage extends DataPanel<List<FriendStatus>>
{
    public FriendsPage (Context ctx)
    {
        super(ctx, "page", "friends");
        _everysvc.getFriends(createCallback());
    }

    @Override // from DataPanel
    protected void init (List<FriendStatus> friends)
    {
        FluentTable table = new FluentTable(5, 0, "handwriting");
        table.setWidth("100%");

        Label label = Widgets.newLabel("Invite friends to play Everything with you:", "machine");
        PushButton invite = ButtonUI.newSmallButton("Invite", new ClickHandler() {
            public void onClick (ClickEvent event) {
                _ctx.displayPopup(new InvitePopup(_ctx, null), null);
            }
        });
        table.add().setWidget(Widgets.newRow(label, invite)).setColSpan(COLUMNS);

        if (friends.size() == 0) {
            return;
        }

        table.add().setText("Your Everything friends:", "machine").setColSpan(COLUMNS);
        int col = 0, row = table.getRowCount();
        for (FriendStatus friend : friends) {
            String lastOnline = DateUtil.formatDateTime(friend.lastSession);
            table.at(row, col).setWidget(XFBML.newProfilePic(friend.name.facebookId)).alignRight().
                right().setWidgets(Args.createInlink(friend.name), Widgets.newLabel(lastOnline));
            col += 2;
            if (col == COLUMNS) {
                row++;
                col = 0;
            }
        }
        add(table);
        XFBML.parse(this);
    }

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
    protected static final int COLUMNS = 6;
}
