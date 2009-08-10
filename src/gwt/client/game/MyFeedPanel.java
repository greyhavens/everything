//
// $Id$

package client.game;

import java.util.ArrayList;
import java.util.List;

import com.threerings.everything.data.FeedItem;
import com.threerings.gwt.ui.Widgets;

import client.ui.XFBML;
import client.util.Context;

/**
 * Displays the current user's feed.
 */
public class MyFeedPanel extends FeedPanel
{
    public MyFeedPanel (Context ctx)
    {
        super(ctx);
        _everysvc.getRecentFeed(createCallback());
    }

    @Override // from DataPanel
    protected void init (List<FeedItem> items)
    {
        List<FeedItem> highlights = new ArrayList<FeedItem>();
        for (FeedItem item : items) {
            if (isHighlight(item)) {
                highlights.add(item);
            }
        }
        if (highlights.size() > 0) {
            add(Widgets.newLabel("Highlights", "Title"));
            while (highlights.size() > 0) {
                add(formatItem(highlights.remove(0), highlights, Mode.HIGHLIGHT));
            }
        }

        add(Widgets.newLabel("Recent Happenings", "Title"));
        while (items.size() > 0) {
            add(formatItem(items.remove(0), items, Mode.NORMAL));
        }

        XFBML.parse(this);
    }

    protected boolean isHighlight (FeedItem item)
    {
        // if we're the target of a gift, or another editor commented on our series, highlight it
        return (_ctx.getMe().equals(item.target) || item.type == FeedItem.Type.COMMENT ||
                // if we completed a series or received a card for our birthday, highlight it
                (_ctx.getMe().equals(item.actor) && (item.type == FeedItem.Type.COMPLETED ||
                                                     item.type == FeedItem.Type.BIRTHDAY)));
    }
}
