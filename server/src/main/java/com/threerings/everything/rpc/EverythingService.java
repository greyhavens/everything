//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.app.client.ServiceException;

import com.threerings.everything.data.Card;
import com.threerings.everything.data.CategoryComment;
import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.PlayerStats;
import com.threerings.everything.data.SessionData;
import com.threerings.everything.data.ThingCard;

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

    /** Thrown by {@link #getUserFeed} if the user in question does not exist. */
    public static final String E_UNKNOWN_USER = "e.unknown_user";

    /** Provides results for {@link #getCredits}. */
    public static class CreditsResult implements IsSerializable
    {
        public PlayerName design;
        public PlayerName art;
        public List<PlayerName> code;
        public List<PlayerName> editors;
    }

    /** Provides results for {@link #getRecentFeed}. */
    public static class FeedResult implements IsSerializable
    {
        /** The "recruitment gifts" for you to use to slurp in new players, or null to mark
         * a slot as already gifted. */
        public List<Card> recruitGifts;

        /** Gifts awaiting this player, if any. */
        public List<ThingCard> gifts;

        /** Comments on this user's series. */
        public List<CategoryComment> comments;

        /** This user's recent feed. */
        public List<FeedItem> items;
    }

    /**
     * Validates that this client has proper session credentials. Returns null if they do not.
     */
    SessionData validateSession (String version, int tzOffset) throws ServiceException;

    /**
     * Returns the calling user's pending gifts and data on their friends' activities.
     */
    FeedResult getRecentFeed () throws ServiceException;

    /**
     * Returns a list of recent activity for the specified user.
     */
    List<FeedItem> getUserFeed (int userId) throws ServiceException;

    /**
     * Returns stats on the caller's friends (the caller is included in this set).
     */
    List<PlayerStats> getFriends () throws ServiceException;

    /**
     * Returns the data needed for the credits page.
     */
    CreditsResult getCredits () throws ServiceException;
}
