//
// $Id$

package client.game;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.client.EverythingServiceAsync;
import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.PlayerName;

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

    protected String getName (PlayerName name, boolean capital)
    {
        return _ctx.getMe().userId != name.userId ? name.name : (capital ? "You" : "you");
    }

    protected class FeedItemLabel extends FlowPanel
    {
        public FeedItemLabel (FeedItem item) {
            setStyleName("Item");
            add(Args.createInlink(getName(item.actor, true), Page.BROWSE, item.actor.userId));
            switch (item.type) {
            case FLIPPED:
                add(Widgets.newHTML(" flipped the " + format(item.objects) + ".", "inline"));
                break;
            case GIFTED:
                add(Widgets.newHTML(" gave the " + format(item.objects) + " to ", "inline"));
                add(Args.createInlink(getName(item.target, false),
                                      Page.BROWSE, item.target.userId));
            }
        }

        protected String format (List<String> objects) {
            StringBuffer buf = new StringBuffer();
            for (Iterator<String> iter = objects.iterator(); iter.hasNext(); ) {
                String object = iter.next();
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                if (!iter.hasNext() && objects.size() > 0) { // yay for English!
                    buf.append("and ");
                }
                buf.append("<b>").append(object).append("</b>");
            }
            return buf.append(objects.size() > 1 ? " cards" : " card").toString();
        }
    }

    protected Context _ctx;

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
}
