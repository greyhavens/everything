//
// $Id$

package com.threerings.everything.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.samsara.app.client.ServiceException;

import com.threerings.everything.client.AdminService;
import com.threerings.everything.data.PlayerDetails;
import com.threerings.everything.data.PlayerFullName;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.ThingRepository;

/**
 * Implements {@link AdminService}.
 */
@Singleton
public class AdminServlet extends EveryServiceServlet
    implements AdminService
{
    // from interface AdminService
    public DashboardResult getDashboard () throws ServiceException
    {
        requireAdmin();

        DashboardResult result = new DashboardResult();
        result.stats = _thingRepo.loadStats();
        result.recentPlayers = Lists.newArrayList(_playerRepo.loadRecentPlayers(MAX_RECENTS));
        result.pendingCategories = Lists.newArrayList(_thingRepo.loadPendingCategories());
        return result;
    }

    // from interface AdminService
    public PlayerDetails getPlayerDetails (int userId) throws ServiceException
    {
        requireAdmin();
        return PlayerRecord.TO_DETAILS.apply(_playerRepo.loadPlayer(userId));
    }

    // from interface AdminService
    public void updateIsEditor (int userId, boolean isEditor) throws ServiceException
    {
        requireAdmin();
        _playerRepo.updateIsEditor(userId, isEditor);
    }

    // from interface AdminService
    public List<PlayerFullName> findPlayers (String query) throws ServiceException
    {
        requireAdmin();
        return Lists.newArrayList(); // TODO
    }

    @Inject protected ThingRepository _thingRepo;

    protected static final int MAX_RECENTS = 10;
}
