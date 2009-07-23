//
// $Id$

package client.game;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HasAlignment;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;

import com.threerings.everything.client.EverythingServiceAsync;
import com.threerings.everything.client.EverythingService;
import com.threerings.everything.data.FriendStatus;

import client.ui.DataPanel;
import client.ui.XFBML;
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
        table.setWidth("100%");
        if (friends.size() == 0) {
            table.setText(0, 0, "You have no friends. This makes us sad.");
            table.setText(1, 0, "Soon we'll provide a way to invite your Facebook friends " +
                          "to come and play!");
            return;
        }

        table.setText(0, 0, "Browse your friends' collections and see whent hey last played.", 6);
        int row = 1, col = 0;
        for (FriendStatus friend : friends) {
            table.setWidget(row, 2*col, XFBML.newProfilePic(friend.name.facebookId));
            table.getFlexCellFormatter().setRowSpan(row, 2*col, 2);
            table.getFlexCellFormatter().setHorizontalAlignment(
                row, 2*col, HasAlignment.ALIGN_RIGHT);
            table.setWidget(row, 2*col+1, Args.createInlink(friend.name));
            table.setText(row+1, col, DateUtil.formatDateTime(friend.lastSession));
            if (++col == 3) {
                row += 2;
                col = 0;
            }
        }
        add(table);
    }

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
}
