//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.data;

import java.io.Serializable;

/**
 * Provides information on a player's game status.
 */
public class GameStatus
    implements Serializable
{
    /** The player's current coin balance. */
    public int coins;

    /** The number of free flips remaining. */
    public int freeFlips;

    /** The cost of the next flip (0 if there are free flips remaining). */
    public int nextFlipCost;
}
