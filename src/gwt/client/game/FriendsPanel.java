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
        SmartTable table = new SmartTable("handwriting", 5, 0);
        table.setWidth("100%");
        if (friends.size() == 0) {
            table.setText(0, 0, "You have no friends. This makes us sad.");
            table.setText(1, 0, "Soon we'll provide a way to invite your Facebook friends " +
                          "to come and play!");
            return;
        }

        table.setText(0, 0, "Browse your friends' collections and see when they last played:",
                      COLUMNS, "machine");
        int row = 1, col = 0;
        for (FriendStatus friend : friends) {
            table.getFlexCellFormatter().setHorizontalAlignment(row, col, HasAlignment.ALIGN_RIGHT);
            table.setWidget(row, col++, XFBML.newProfilePic(friend.name.facebookId));
            String lastOnline = DateUtil.formatDateTime(friend.lastSession);
            table.setWidget(row, col++, Widgets.newFlowPanel(Args.createInlink(friend.name),
                                                             Widgets.newLabel(lastOnline)));
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
