//
// $Id$

package com.threerings.everything.client;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.samsara.app.client.ServiceException;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.News;
import com.threerings.everything.data.PlayerDetails;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.ThingStats;

/**
 * Provides admin services to the Everything client.
 */
@RemoteServiceRelativePath(AdminService.ENTRY_POINT)
public interface AdminService extends RemoteService
{
    /** The path at which this servlet is mapped. */
    public static final String ENTRY_POINT = "admin";

    /** An error reported by {@link #updateIsEditor} and {@link #getPlayerDetails}. */
    public static final String E_UNKNOWN_USER = "e.unknown_user";

    /** Provides results for {@link #getDashboard}. */
    public static class DashboardResult implements IsSerializable
    {
        /** The status of our thing database. */
        public ThingStats stats;

        /** Categories that have not been activated. */
        public List<Category> pendingCategories;

        /** Players that have recently joined the game. */
        public List<PlayerName> recentPlayers;

        /** The latest news or null. */
        public News latestNews;
    }

    /**
     * Returns the current admin dashboard data.
     */
    DashboardResult getDashboard () throws ServiceException;

    /**
     * Returns details for the specified player.
     */
    PlayerDetails getPlayerDetails (int userId) throws ServiceException;

    /**
     * Updates this player's editor status.
     */
    void updateIsEditor (int userId, boolean isEditor) throws ServiceException;

    /**
     * Looks up players by first or last name.
     */
    List<PlayerName> findPlayers (String query) throws ServiceException;

    /**
     * Adds a new news report. Returns the time assigned to this news report.
     */
    long addNews (String text) throws ServiceException;

    /**
     * Updates an existing news report.
     */
    void updateNews (long reported, String text) throws ServiceException;

    /**
     * Grants freebie coins to the specified player.
     */
    void grantCoins (int userId, int coins) throws ServiceException;
}
