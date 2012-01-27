//
// $Id$

package com.threerings.everything.rpc;

import java.util.List;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.PlayerStats;
import com.threerings.everything.data.SessionData;

/**
 * Provides the asynchronous version of {@link EverythingService}.
 */
public interface EverythingServiceAsync
{
    /**
     * The async version of {@link EverythingService#validateSession}.
     */
    void validateSession (String version, int tzOffset, String kontagentToken, AsyncCallback<SessionData> callback);

    /**
     * The async version of {@link EverythingService#getRecentFeed}.
     */
    void getRecentFeed (AsyncCallback<EverythingService.FeedResult> callback);

    /**
     * The async version of {@link EverythingService#getUserFeed}.
     */
    void getUserFeed (int userId, AsyncCallback<List<FeedItem>> callback);

    /**
     * The async version of {@link EverythingService#getFriends}.
     */
    void getFriends (AsyncCallback<List<PlayerStats>> callback);

    /**
     * The async version of {@link EverythingService#getCredits}.
     */
    void getCredits (AsyncCallback<EverythingService.CreditsResult> callback);

    /**
     * The async version of {@link EverythingService#storyPosted}.
     */
    void storyPosted (String tracking, AsyncCallback<Void> callback);
}
