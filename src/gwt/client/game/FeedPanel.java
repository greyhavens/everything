//
// $Id$

package client.game;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

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
            add(formatItem(item));
        }
        XFBML.parse(this);
    }

    protected Widget formatItem (FeedItem item)
    {
        FlowPanel action = Widgets.newFlowPanel("Action");
        action.add(Args.createInlink(getName(item.actor, true),
                                     Page.BROWSE, item.actor.userId));
        String objmsg, comtitle = null;
        switch (item.type) {
        case FLIPPED:
            objmsg = format(item.objects, "card", "cards");
            action.add(Widgets.newHTML(" flipped the " + objmsg + ".", "inline"));
            comtitle = item.actor + " flipped the " + objmsg + ".";
            break;
        case GIFTED:
            objmsg = format(item.objects, "card", "cards");
            action.add(Widgets.newHTML(" gave the " + objmsg + " to ", "inline"));
            action.add(Args.createInlink(getName(item.target, false),
                                         Page.BROWSE, item.target.userId));
            String post = (item.message == null) ? "." :
                ". " + item.actor.name + " said \"" + item.message + "\"";
            action.add(Widgets.newInlineLabel(post));
            comtitle = item.actor + " gave the  " + objmsg + " to " + item.target + ".";
            break;
        case COMMENT:
            action.add(Widgets.newInlineLabel(" commented on your category "));
            action.add(Args.createInlink(item.objects.get(0), Page.EDIT_SERIES, item.message));
            break;
        case COMPLETED:
            objmsg = format(item.objects, "series", "series");
            action.add(Widgets.newHTML(" completed the " + objmsg + "!", "inline"));
            comtitle = item.actor + " completed the " + objmsg + "!";
            break;
        default:
            action.add(Widgets.newInlineLabel(" did something mysterious."));
            break;
        }
        action.add(Widgets.newLabel(DateUtil.formatDateTime(item.when), "When"));
// need to find out how to just show _Comments_ and expand it only if there are comments
//         if (comtitle != null) {
//             String xid = "feed:" + item.actor.facebookId + ":" + item.when.getTime();
//             action.add(XFBML.newCommentsBox(xid, comtitle, 3));
//         }
        return Widgets.newRow(HasAlignment.ALIGN_TOP, "Item",
                              XFBML.newProfilePic(item.actor.facebookId), action);
    }

    protected String getName (PlayerName name, boolean capital)
    {
        return _ctx.getMe().equals(name) ? (capital ? "You" : "you") : name.toString();
    }

    protected String format (List<String> objects, String what, String pwhat)
    {
        StringBuilder buf = new StringBuilder();
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

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
}
