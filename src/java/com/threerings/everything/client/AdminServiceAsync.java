//
// $Id$

package com.threerings.everything.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.everything.data.Player;
import com.threerings.everything.data.PlayerName;

/**
 * Provides the asynchronous version of {@link AdminService}.
 */
public interface AdminServiceAsync
{
    /**
     * The async version of {@link AdminService#getStats}.
     */
    void getStats (AsyncCallback<AdminService.StatsResult> callback);

    /**
     * The async version of {@link AdminService#getRegiStats}.
     */
    void getRegiStats (AsyncCallback<AdminService.RegiStatsResult> callback);

    /**
     * The async version of {@link AdminService#getPlayerDetails}.
     */
    void getPlayerDetails (int userId, AsyncCallback<Player> callback);

    /**
     * The async version of {@link AdminService#updateIsEditor}.
     */
    void updateIsEditor (int userId, boolean isEditor, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#findPlayers}.
     */
    void findPlayers (String query, AsyncCallback<List<PlayerName>> callback);

    /**
     * The async version of {@link AdminService#addNews}.
     */
    void addNews (String text, AsyncCallback<Long> callback);

    /**
     * The async version of {@link AdminService#updateNews}.
     */
    void updateNews (long reported, String text, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#grantCoins}.
     */
    void grantCoins (int userId, int coins, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#grantFreeFlips}.
     */
    void grantFreeFlips (int userId, int flips, AsyncCallback<Void> callback);
}
