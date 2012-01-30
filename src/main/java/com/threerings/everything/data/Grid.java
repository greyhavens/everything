//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains a player's current card grid and game status.
 */
public class Grid
    implements IsSerializable
{
    /** The number of things that are placed in a grid when it is generated. */
    public static final int GRID_SIZE = 16;

    /** The id assigned to this grid. */
    public int gridId;

    /** The current status of this grid. */
    public GridStatus status;

    /** Info on the flipped status of each slot. */
    public SlotStatus[] slots;

    /** Info on the flipped cards in each position (null or partial card at unflipped positions). */
    public ThingCard[] flipped;

    /** Counts of rarities of all unflipped cards. */
    public int[] unflipped;

    /** The time at which this grid expires and will be replaced. */
    public Date expires;

    /** Returns true if we've flipped any of our cards. */
    public boolean haveFlipped () {
        for (SlotStatus slot : slots) {
            if (slot != SlotStatus.UNFLIPPED) {
                return true;
            }
        }
        return false;
    }
}
