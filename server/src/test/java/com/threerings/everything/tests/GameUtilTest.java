//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.tests;

import org.junit.*;
import static org.junit.Assert.*;

import com.threerings.everything.server.GameCodes;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.util.GameUtil;

/**
 * Tests game util methods.
 */
public class GameUtilTest
{
    @Test public void testComputeFreeFlips ()
    {
        long elapsed = GameUtil.ONE_DAY;
        int expect = GameCodes.DAILY_FREE_FLIPS;
        assertEquals(GameUtil.computeFreeFlips(getTester(), elapsed), expect);

        elapsed = 2*GameUtil.ONE_DAY;
        expect = GameCodes.DAILY_FREE_FLIPS + GameCodes.VACATION_FREE_FLIPS;
        assertEquals(GameUtil.computeFreeFlips(getTester(), elapsed), expect);

        elapsed = 5*GameUtil.ONE_DAY;
        expect = GameCodes.DAILY_FREE_FLIPS + 4*GameCodes.VACATION_FREE_FLIPS;
        assertEquals(GameUtil.computeFreeFlips(getTester(), elapsed), expect);

        elapsed = 500*GameUtil.ONE_DAY;
        expect = GameCodes.MAX_FREE_FLIPS;
        assertEquals(GameUtil.computeFreeFlips(getTester(), elapsed), expect);
    }

    protected static PlayerRecord getTester ()
    {
        PlayerRecord who = new PlayerRecord();
        who.name = "Tester";
        who.userId = 1;
        return who;
    }
}
