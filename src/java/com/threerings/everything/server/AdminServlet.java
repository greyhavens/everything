//
// $Id$

package com.threerings.everything.server;

import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;

import com.threerings.everything.client.AdminService;
import com.threerings.everything.data.News;
import com.threerings.everything.data.PlayerDetails;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.server.persist.GameRepository;
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
        for (News news : _gameRepo.loadLatestNews()) {
            result.latestNews = news;
        }
        return result;
    }

    // from interface AdminService
    public PlayerDetails getPlayerDetails (int userId) throws ServiceException
    {
        requireAdmin();
        PlayerRecord player = _playerRepo.loadPlayer(userId);
        if (player == null) {
            throw new ServiceException(E_UNKNOWN_USER);
        }
        return PlayerRecord.TO_DETAILS.apply(player);
    }

    // from interface AdminService
    public void updateIsEditor (int userId, boolean isEditor) throws ServiceException
    {
        OOOUser user = requireAdmin();
        PlayerName player = _playerRepo.loadPlayerName(userId);
        if (player == null) {
            throw new ServiceException(E_UNKNOWN_USER);
        }
        _playerRepo.updateIsEditor(userId, isEditor);
        _adminLogic.noteAction(user.userId, isEditor ? "editored" : "deeditored", player);
    }

    // from interface AdminService
    public List<PlayerName> findPlayers (String query) throws ServiceException
    {
        requireAdmin();
        return Lists.newArrayList(); // TODO
    }

    // from interface AdminService
    public long addNews (String text) throws ServiceException
    {
        OOOUser user = requireAdmin();
        long reported = _gameRepo.reportNews(user.userId, text);
        _adminLogic.noteAction(user.userId, "added news " + new Date(reported));
        return reported;
    }

    // from interface AdminService
    public void updateNews (long reported, String text) throws ServiceException
    {
        OOOUser user = requireAdmin();
        _gameRepo.updateNews(reported, text);
        _adminLogic.noteAction(user.userId, "updated news " + new Date(reported));
    }

    // from interface AdminService
    public void grantCoins (int userId, int coins) throws ServiceException
    {
        OOOUser user = requireAdmin();
        PlayerRecord target = _playerRepo.loadPlayer(userId);
        if (target == null) {
            throw new ServiceException(E_UNKNOWN_USER);
        }
        _playerRepo.grantCoins(userId, coins);
        _adminLogic.noteAction(user.userId, "granted " + coins + " coins to ", target.getName());
    }

    @Inject protected AdminLogic _adminLogic;
    @Inject protected GameRepository _gameRepo;
    @Inject protected ThingRepository _thingRepo;

    protected static final int MAX_RECENTS = 10;
}
