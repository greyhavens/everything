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

/**
 * Displays a feed.
 */
public abstract class FeedPanel extends DataPanel<List<FeedItem>>
{
    protected FeedPanel (Context ctx)
    {
        super(ctx, "feed");
    }

    protected Widget formatItem (FeedItem item, List<FeedItem> items, Mode mode)
    {
        FlowPanel action = Widgets.newFlowPanel("Action");
        action.add(Args.createInlink(getName(item.actor, true),
                                     Page.BROWSE, item.actor.userId));
        String objmsg;
        switch (item.type) {
        case FLIPPED:
            objmsg = format(item.objects, "card", "cards");
            action.add(Widgets.newHTML(" flipped the " + objmsg + ".", "inline"));
            break;
        case GIFTED:
            action.add(Widgets.newInlineLabel(" gave "));
            addGift(action, item);
            if (item.message == null) {
                int idate = DateUtil.getDayOfMonth(item.when);
                for (int ii = 0; ii < items.size(); ii++) {
                    FeedItem eitem = items.get(ii);
                    if (eitem.actor.equals(item.actor) && eitem.type == item.type &&
                        eitem.message == null && DateUtil.getDayOfMonth(eitem.when) == idate) {
                        action.add(Widgets.newInlineLabel(", "));
                        addGift(action, eitem);
                        items.remove(ii--);
                    }
                }
            }
            action.add(Widgets.newInlineLabel("."));
            if (item.message != null) {
                String msg = " " + getFirstName(item.actor) + " said \"" + item.message + "\"";
                action.add(Widgets.newInlineLabel(msg));
            }
            break;
        case COMMENT:
            action.add(Widgets.newInlineLabel(" commented on your category "));
            action.add(Args.createInlink(item.objects.get(0), Page.EDIT_SERIES, item.message));
            break;
        case COMPLETED:
            objmsg = format(item.objects, "series", "series");
            action.add(Widgets.newHTML(" completed the " + objmsg + "!", "inline"));
            break;
        case NEW_SERIES:
            objmsg = format(item.objects, "series", "series");
            action.add(Widgets.newHTML(" added the " + objmsg + ".", "inline"));
            break;
        case BIRTHDAY:
            objmsg = format(item.objects, "card", "cards");
            action.add(Widgets.newHTML(" got the " + objmsg + " as a birthday present.", "inline"));
            break;
        case JOINED:
            action.add(Widgets.newHTML(" started playing Everything. Yay!", "inline"));
            break;
        default:
            action.add(Widgets.newInlineLabel(" did something mysterious."));
            break;
        }

        if (mode != Mode.HIGHLIGHT) {
            action.add(Widgets.newLabel(DateUtil.formatDateTime(item.when), "When"));
        }

        switch (mode) {
        default:
        case NORMAL:
            return Widgets.newRow(HasAlignment.ALIGN_TOP, null,
                                  XFBML.newProfilePic(item.actor.facebookId), action);
        case USER:
            return Widgets.newSimplePanel("Photoless", action);
        case HIGHLIGHT:
            return action;
        }
    }

    protected void addGift (FlowPanel action, FeedItem item)
    {
        String objmsg = format(item.objects, "card", "cards");
        action.add(Widgets.newHTML("the " + objmsg + " to ", "inline"));
        action.add(Args.createInlink(getName(item.target, false), Page.BROWSE, item.target.userId));
    }

    protected String getName (PlayerName name, boolean capital)
    {
        return _ctx.getMe().equals(name) ? (capital ? "You" : "you") : name.toString();
    }

    protected String getFirstName (PlayerName name)
    {
        return _ctx.getMe().equals(name) ? "You" : name.name;
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

    protected enum Mode { NORMAL, USER, HIGHLIGHT };

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
}
