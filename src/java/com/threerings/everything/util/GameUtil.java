//
// $Id$

package com.threerings.everything.util;

import com.threerings.everything.client.GameCodes;
import com.threerings.everything.server.persist.PlayerRecord;

import static com.threerings.everything.Log.log;

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
        if (elapsed < shortDay) {
            log.warning("Requested to compute free flips with less than 18 hours elapsed?",
                        "who", record.who(),  "elapsed", elapsed);
        }

        // if you have free flips left over, we only top you up (but we'll always give you at least
        // vacation_free_flips for your elapsed day)
        int freeFlips = Math.max(GameCodes.DAILY_FREE_FLIPS - record.freeFlips,
                                 GameCodes.VACATION_FREE_FLIPS);
        elapsed -= ONE_DAY;
        while (elapsed > shortDay) { // accomodate the occasional lost hour due to DST
            freeFlips += GameCodes.VACATION_FREE_FLIPS;
            elapsed -= ONE_DAY;
        }

        return Math.min(GameCodes.MAX_FREE_FLIPS, freeFlips);
    }
}
