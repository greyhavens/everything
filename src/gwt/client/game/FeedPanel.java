//
// $Id$

package client.game;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.client.EverythingServiceAsync;
import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.PlayerName;

import client.ui.XFBML;
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
        for (FeedItem item : items) {
            add(new FeedItemLabel(item));
        }
        XFBML.parse(this);
    }

    protected String getName (PlayerName name, boolean capital)
    {
        return _ctx.getMe().equals(name) ? (capital ? "You" : "you") : name.toString();
    }

    protected class FeedItemLabel extends SmartTable
    {
        public FeedItemLabel (FeedItem item) {
            super("Item", 5, 0);
            setWidget(0, 0, XFBML.newProfilePic(item.actor.facebookId));
            getFlexCellFormatter().setRowSpan(0, 0, 2);
            FlowPanel action = new FlowPanel();
            action.add(Args.createInlink(getName(item.actor, true),
                                         Page.BROWSE, item.actor.userId));
            String objmsg;
            switch (item.type) {
            case FLIPPED:
                objmsg = format(item.objects, "card", "cards");
                action.add(Widgets.newHTML(" flipped the " + objmsg + ".", "inline"));
                break;
            case GIFTED:
                objmsg = format(item.objects, "card", "cards");
                action.add(Widgets.newHTML(" gave the " + objmsg + " to ", "inline"));
                action.add(Args.createInlink(getName(item.target, false),
                                      Page.BROWSE, item.target.userId));
                String post = (item.message == null) ? "." :
                    ". " + item.actor.name + " said \"" + item.message + "\"";
                action.add(Widgets.newInlineLabel(post));
                break;
            case COMMENT:
                action.add(Widgets.newInlineLabel(" commented on your category "));
                action.add(Args.createInlink(item.objects.get(0), Page.EDIT_SERIES, item.message));
                break;
            case COMPLETED:
                objmsg = format(item.objects, "series", "series");
                action.add(Widgets.newHTML(" completed the " + objmsg + "!", "inline"));
                break;
            default:
                action.add(Widgets.newInlineLabel(" did something mysterious."));
                break;
            }
            setWidget(0, 1, action, 1, "Action");
            setText(1, 0, DateUtil.formatDateTime(item.when), 1, "When");
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
