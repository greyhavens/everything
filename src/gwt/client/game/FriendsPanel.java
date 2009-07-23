//
// $Id$

package client.game;

import java.util.List;

import com.google.gwt.core.client.GWT;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;

import com.threerings.everything.client.EverythingServiceAsync;
import com.threerings.everything.client.EverythingService;
import com.threerings.everything.data.FriendStatus;

import client.ui.DataPanel;
import client.util.Args;
import client.util.Context;

/**
 * Displays all of a player's friends and allows them to browse their collections.
 */
public class FriendsPanel extends DataPanel<List<FriendStatus>>
{
    public FriendsPanel (Context ctx)
    {
        super(ctx, "page", "friends");
        _everysvc.getFriends(createCallback());
    }

    @Override // from DataPanel
    protected void init (List<FriendStatus> friends)
    {
        SmartTable table = new SmartTable(5, 0);
        if (friends.size() == 0) {
            table.setText(0, 0, "You have no friends. This makes us sad.");
            table.setText(1, 0, "Soon we'll provide a way to invite your Facebook friends " +
                          "to come and play!");
        } else {
            table.setText(0, 0, "Friend", 1, "machine");
            table.setText(0, 1, "Last played", 1, "machine");
            for (FriendStatus friend : friends) {
                int row = table.addWidget(Args.createInlink(friend.name), 1, null);
                table.setText(row, 1, DateUtil.formatDateTime(friend.lastSession));
            }
        }
        add(table);
    }

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
}
