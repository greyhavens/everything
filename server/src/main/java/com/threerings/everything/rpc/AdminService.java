//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.rpc;

import java.util.Date;
import java.util.List;
import java.util.SortedMap;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.app.client.ServiceException;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.Player;
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

    /** Provides results for {@link #getStats}. */
    public static class StatsResult implements IsSerializable
    {
        /** The status of our thing database. */
        public ThingStats stats;

        /** Categories that have not been activated. */
        public List<Category> pendingCategories;
    }

    /** Provides results for {@link #getRegiStats}. */
    public static class RegiStatsResult implements IsSerializable
    {
        /** A map of registrations per day. */
        public SortedMap<Date, Integer> regcounts;

        /** Recent registrants. */
        public List<PlayerName> players;
    }

    /**
     * Returns the current admin stats data.
     */
    StatsResult getStats () throws ServiceException;

    /**
     * Returns data on recent registration activity.
     */
    RegiStatsResult getRegiStats () throws ServiceException;

    /**
     * Returns details for the specified player.
     */
    Player getPlayerDetails (int userId) throws ServiceException;

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

    /**
     * Grants free flips to the specified player, or to all players if userId is zero.
     */
    void grantFreeFlips (int userId, int flips) throws ServiceException;
}
