//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server;

import java.util.List;

import com.google.inject.Inject;

import com.threerings.app.client.ServiceException;

import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.PlayerStats;
import com.threerings.everything.data.SessionData;
import com.threerings.everything.rpc.EverythingService;

/**
 * Implements {@link EverythingService}.
 */
public class EverythingServlet extends EveryServiceServlet
    implements EverythingService
{
    // from interface EverythingService
    public SessionData validateSession (String version, int tzOffset) throws ServiceException {
        return _logic.validateSession(getThreadLocalRequest(), getUser(), tzOffset, false);
    }

    // from interface EverythingService
    public FeedResult getFeed () throws ServiceException {
        return _logic.getFeed(requirePlayer());
    }

    // from interface EverythingService
    public List<FeedItem> getUserFeed (int userId) throws ServiceException {
        return _logic.getUserFeed(getPlayer(), userId);
    }

    // from interface EverythingService
    public List<PlayerStats> getFriends () throws ServiceException {
        return _logic.getFriends(requirePlayer());
    }

    // from interface EverythingService
    public CreditsResult getCredits () throws ServiceException {
        return _logic.getCredits();
    }

    // from interface EverythingService
    public void storyPosted (String tracking) throws ServiceException {
        // we used to report this for tracking, now we don't
    }

    @Inject protected EverythingLogic _logic;
}
