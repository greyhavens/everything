//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.client.game;

import java.util.List;

import com.threerings.everything.data.FeedItem;

import com.threerings.everything.client.util.Context;

/**
 * Displays a particular user's recent activities.
 */
public class UserFeedPanel extends FeedPanel<List<FeedItem>>
{
    public UserFeedPanel (Context ctx, int userId)
    {
        super(ctx);
        _everysvc.getUserFeed(userId, createCallback());
    }

    @Override // from DataPanel
    protected void init (List<FeedItem> items)
    {
        while (items.size() > 0) {
            add(formatItem(items.remove(0), items, Mode.USER));
        }
    }
}
