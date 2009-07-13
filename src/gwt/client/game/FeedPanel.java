//
// $Id$

package client.game;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.client.EverythingServiceAsync;
import com.threerings.everything.data.FeedItem;

import client.util.Args;
import client.util.Context;
import client.util.Page;
import client.util.PanelCallback;

/**
 * Displays the player's recent feed.
 */
public class FeedPanel extends FlowPanel
{
    public FeedPanel (Context ctx)
    {
        setStyleName("feed");
        add(Widgets.newLabel("Loading...", null));

        _ctx = ctx;
        _everysvc.getRecentFeed(new PanelCallback<List<FeedItem>>(this) {
            public void onSuccess (List<FeedItem> items) {
                clear();
                init(items);
            }
        });
    }

    protected void init (List<FeedItem> items)
    {
        String date = "Today";

        for (FeedItem item : items) {
            String idate = DateUtil.formatDate(item.when);
            if (!idate.equals(date)) {
                add(Widgets.newLabel(idate, "Date"));
            }
            add(new FeedItemLabel(item));
        }
    }

    protected static class FeedItemLabel extends FlowPanel
    {
        public FeedItemLabel (FeedItem item) {
            setStyleName("Item");
            add(Args.createInlink(item.actor.name, Page.BROWSE, item.actor.userId));
            switch (item.type) {
            case FLIPPED:
                add(Widgets.newHTML(" flipped the <b>" + item.object + "</b> card.", "inline"));
                break;
            case GIFTED:
                add(Widgets.newHTML(" gave the <b>" + item.object + "</b> card to ", "inline"));
                add(Args.createInlink(item.target.name, Page.BROWSE, item.actor.userId));
            }
        }
    }

    protected Context _ctx;

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
}
