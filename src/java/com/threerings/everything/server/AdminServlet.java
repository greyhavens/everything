//
// $Id$

package com.threerings.everything.server;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;

import com.threerings.everything.client.AdminService;
import com.threerings.everything.data.Player;
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
    public StatsResult getStats () throws ServiceException
    {
        requireAdmin();
        StatsResult result = new StatsResult();
        result.stats = _thingRepo.loadStats();
        result.pendingCategories = _thingRepo.loadNonActiveCategories().toList();
        Collections.sort(result.pendingCategories);
        return result;
    }

    // from interface AdminService
    public RegiStatsResult getRegiStats () throws ServiceException
    {
        requireAdmin();
        RegiStatsResult result = new RegiStatsResult();
        result.regcounts = _playerRepo.summarizeRegis(REG_COUNT_DAYS);
        result.players = _playerRepo.loadRecentPlayers(MAX_RECENTS).toList();
        return result;
    }

    // from interface AdminService
    public List<PlayerName> getRecentPlayers () throws ServiceException
    {
        requireAdmin();
        return _playerRepo.loadRecentPlayers(MAX_RECENTS).toList();
    }

    // from interface AdminService
    public Player getPlayerDetails (int userId) throws ServiceException
    {
        requireAdmin();
        PlayerRecord player = requirePlayer(userId);
        return PlayerRecord.TO_PLAYER.apply(player);
    }

    // from interface AdminService
    public void updateIsEditor (int userId, boolean isEditor) throws ServiceException
    {
        OOOUser user = requireAdmin();
        PlayerName player = requirePlayer(userId).getName();
        _playerRepo.updateIsEditor(userId, isEditor);
        _adminLogic.noteAction(user.userId, isEditor ? "editored" : "deeditored", player);
    }

    // from interface AdminService
    public List<PlayerName> findPlayers (String query) throws ServiceException
    {
        requireAdmin();
        return _playerRepo.findPlayers(query).toList();
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
        PlayerRecord target = requirePlayer(userId);
        _playerRepo.grantCoins(userId, coins);
        _adminLogic.noteAction(user.userId, "granted " + coins + " coins to ", target.getName());
    }

    // from interface AdminService
    public void grantFreeFlips (int userId, int flips) throws ServiceException
    {
        OOOUser user = requireAdmin();
        if (userId == 0) {
            _playerRepo.grantFreeFlipsToEveryone(flips);
            _adminLogic.noteAction(user.userId, "grantd " + flips + " to everyone");
        } else {
            PlayerRecord target = requirePlayer(userId);
            _playerRepo.grantFreeFlips(target, flips);
            _adminLogic.noteAction(
                user.userId, "granted " + flips + " free flips to ", target.getName());
        }
    }

    protected PlayerRecord requirePlayer (int userId)
        throws ServiceException
    {
        PlayerRecord target = _playerRepo.loadPlayer(userId);
        if (target == null) {
            throw new ServiceException(E_UNKNOWN_USER);
        }
        return target;
    }

    @Inject protected AdminLogic _adminLogic;
    @Inject protected GameRepository _gameRepo;
    @Inject protected ThingRepository _thingRepo;

    protected static final int REG_COUNT_DAYS = 21;
    protected static final int MAX_RECENTS = 21;
}
