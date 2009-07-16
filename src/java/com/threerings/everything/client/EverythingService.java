//
// $Id$

package com.threerings.everything.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.samsara.app.client.ServiceException;

import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.FriendStatus;
import com.threerings.everything.data.SessionData;

/**
 * Defines the services available to the Everything client.
 */
@RemoteServiceRelativePath(EverythingService.ENTRY_POINT)
public interface EverythingService extends RemoteService
{
    /** The path at which this servlet is mapped. */
    public static final String ENTRY_POINT = "everything";

    /** Thrown by {@link #validateSession} if we can't communicate with Facebook. */
    public static final String E_FACEBOOK_DOWN = "e.facebook_down";

    /**
     * Validates that this client has proper session credentials. Returns null if they do not.
     */
    SessionData validateSession (String version, int tzOffset) throws ServiceException;

    /**
     * Returns a list of recent activity for the calling user.
     */
    List<FeedItem> getRecentFeed () throws ServiceException;

    /**
     * Returns the names of the caller's friends.
     */
    List<FriendStatus> getFriends () throws ServiceException;
}
