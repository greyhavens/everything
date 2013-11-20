//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.util;

import com.threerings.everything.data.Player;
import com.threerings.everything.server.GameCodes;
import com.threerings.everything.server.persist.PlayerRecord;

/**
 * Game related utility methods.
 */
public class GameUtil
{
    /** One day in milliseconds. */
    public static final long ONE_DAY = 24 * 60 * 60 * 1000L;

    /**
     * Returns the number of free flips to grant to a player for whom the supplied number of
     * milliseconds have elapsed between the expirey of their old grid and their new grid.
     */
    public static int computeFreeFlips (PlayerRecord record, long elapsed)
    {
        long shortDay = 3*ONE_DAY/4;

        // if you have free flips left over, we only top you up (but we'll always give you at least
        // vacation_free_flips for your elapsed day)
        int freeFlips = Math.max(GameCodes.DAILY_FREE_FLIPS - record.freeFlips,
                                 GameCodes.VACATION_FREE_FLIPS);
        elapsed -= ONE_DAY;
        while (elapsed > shortDay) { // accomodate the occasional lost hour due to DST
            freeFlips += GameCodes.VACATION_FREE_FLIPS;
            elapsed -= ONE_DAY;
        }

        // if the player has the EXTRA_FLIP powerup, give them one more flip
        if (record.isSet(Player.Flag.EXTRA_FLIP)) {
            freeFlips += 1;
        }

        // cap them at MAX_FREE_FLIPS (including existing free flips), but don't subtract flips for
        // players that have accumulated more than MAX_FREE_FLIPS
        return Math.max(0, Math.min(GameCodes.MAX_FREE_FLIPS - record.freeFlips, freeFlips));
    }
}
