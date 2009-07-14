//
// $Id$

package com.threerings.everything.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.everything.data.PlayerDetails;
import com.threerings.everything.data.PlayerName;

/**
 * Provides the asynchronous version of {@link AdminService}.
 */
public interface AdminServiceAsync
{
    /**
     * The async version of {@link AdminService#getDashboard}.
     */
    void getDashboard (AsyncCallback<AdminService.DashboardResult> callback);

    /**
     * The async version of {@link AdminService#getPlayerDetails}.
     */
    void getPlayerDetails (int userId, AsyncCallback<PlayerDetails> callback);

    /**
     * The async version of {@link AdminService#updateIsEditor}.
     */
    void updateIsEditor (int userId, boolean isEditor, AsyncCallback<Void> callback);

    /**
     * The async version of {@link AdminService#findPlayers}.
     */
    void findPlayers (String query, AsyncCallback<List<PlayerName>> callback);
}
