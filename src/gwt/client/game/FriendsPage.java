//
// $Id$

package client.game;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.ui.SmartTable;
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
        SmartTable table = new SmartTable("handwriting", 5, 0);
        table.setWidth("100%");

        Label label = Widgets.newLabel("Invite friends to play Everything with you:", "machine");
        table.addWidget(Widgets.newRow(label, ButtonUI.newSmallButton("Invite", new ClickHandler() {
            public void onClick (ClickEvent event) {
                _ctx.displayPopup(new InvitePopup(_ctx, null), null);
            }
        })), COLUMNS);

        if (friends.size() == 0) {
            return;
        }

        int col = 0, row = table.addText("Your Everything friends:", COLUMNS, "machine")+1;
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
