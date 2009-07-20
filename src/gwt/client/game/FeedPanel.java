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

import client.ui.DataPanel;
import client.util.Args;
import client.util.Context;
import client.util.Page;
import client.util.PanelCallback;

/**
 * Displays the player's recent feed.
 */
public class FeedPanel extends DataPanel<List<FeedItem>>
{
    public FeedPanel (Context ctx)
    {
        super(ctx, "feed");
        _everysvc.getRecentFeed(createCallback());
    }

    @Override // from DataPanel
    protected void init (List<FeedItem> items)
    {
        String date = "Today";
        for (FeedItem item : items) {
            String idate = DateUtil.formatDate(item.when);
            if (!idate.equals(date)) {
                add(Widgets.newLabel(idate, "Date"));
                date = idate;
            }
            add(new FeedItemLabel(item));
        }
    }

    protected String getName (PlayerName name, boolean capital)
    {
        return _ctx.getMe().equals(name) ? (capital ? "You" : "you") : name.toString();
    }

    protected class FeedItemLabel extends FlowPanel
    {
        public FeedItemLabel (FeedItem item) {
            setStyleName("Item");
            add(Args.createInlink(getName(item.actor, true), Page.BROWSE, item.actor.userId));
            String objmsg;
            switch (item.type) {
            case FLIPPED:
                objmsg = format(item.objects, "card", "cards");
                add(Widgets.newHTML(" flipped the " + objmsg + ".", "inline"));
                break;
            case GIFTED:
                objmsg = format(item.objects, "card", "cards");
                add(Widgets.newHTML(" gave the " + objmsg + " to ", "inline"));
                add(Args.createInlink(getName(item.target, false),
                                      Page.BROWSE, item.target.userId));
                String post = (item.message == null) ? "." :
                    ". " + item.actor.name + " said \"" + item.message + "\"";
                add(Widgets.newInlineLabel(post));
                break;
            case COMMENT:
                add(Widgets.newInlineLabel(" commented on your category "));
                add(Args.createInlink(item.objects.get(0), Page.EDIT_SERIES, item.message));
                break;
            case COMPLETED:
                objmsg = format(item.objects, "series", "series");
                add(Widgets.newHTML(" completed the " + objmsg + "!", "inline"));
                break;
            default:
                add(Widgets.newInlineLabel(" did something mysterious."));
                break;
            }
        }

        protected String format (List<String> objects, String what, String pwhat) {
            StringBuffer buf = new StringBuffer();
            for (Iterator<String> iter = objects.iterator(); iter.hasNext(); ) {
                String object = iter.next();
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                if (!iter.hasNext() && objects.size() > 1) { // yay for English!
                    buf.append("and ");
                }
                buf.append("<b>").append(object).append("</b>");
            }
            return buf.append(" ").append(objects.size() > 1 ? pwhat : what).toString();
        }
    }

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
}
