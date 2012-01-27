//
// $Id$

package com.threerings.everything.tests;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.util.StringUtil;

import com.threerings.app.client.ServiceException;

import com.threerings.everything.data.Powerup;
import com.threerings.everything.server.GameLogic;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;

import static com.threerings.everything.Log.log;

/**
 * A standalone tool for testing grid generation.
 */
public class TestSelectGridThings extends TestBase
{
    public static void main (String[] args)
    {
        run(TestSelectGridThings.class, args);
    }

    public void run (String[] args)
    {
        int userId;
        Powerup pup;
        try {
            userId = Integer.parseInt(args[0]);
        } catch (Exception e) {
            usage(null);
            return;
        }
        try {
            pup = (args.length > 1) ? Powerup.valueOf(args[1].toUpperCase()) : null;
        } catch (Exception e) {
            usage("Valid powerups: " + StringUtil.toString(Powerup.values()));
            return;
        }

        PlayerRecord player = _playerRepo.loadPlayer(userId);
        if (player == null) {
            log.warning("Unknown player", "id", userId);
            return;
        }

        log.info("Selecting grid", "for", player.who(), "pup", pup);
        try {
            log.info("Selected grid",
                "grid", _gameLogic.selectGridThings(player, pup, Maps.<Integer, Float>newHashMap()));
        } catch (ServiceException se) {
            log.warning("Failed to select grid", "cause", se.getMessage());
        }
    }

    protected static void usage (String errmsg)
    {
        usage("TestSelectGridThings userId [powerup]", errmsg);
    }

    @Inject protected GameLogic _gameLogic;
    @Inject protected PlayerRepository _playerRepo;
}
