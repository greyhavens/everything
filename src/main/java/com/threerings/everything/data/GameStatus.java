//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Provides information on a player's game status.
 */
public class GameStatus
    implements IsSerializable
{
    /** The player's current coin balance. */
    public int coins;

    /** The number of free flips remaining. */
    public int freeFlips;

    /** The cost of the next flip (0 if there are free flips remaining). */
    public int nextFlipCost;
}
