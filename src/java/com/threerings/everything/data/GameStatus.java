//
// $Id$

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

    /** The time at which this player's next free flip will be awarded. */
    public long nextFreeFlipAt;
}
