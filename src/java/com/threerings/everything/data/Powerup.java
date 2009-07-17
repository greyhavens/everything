//
// $Id$

package com.threerings.everything.data;

import com.samskivert.depot.ByteEnum;

/**
 * Defines our different powerup types.
 */
public enum Powerup implements ByteEnum
{
    //
    // consumable powerups (id starts at 1)

    /** Reveals the category of all cards in the grid. */
    SHOW_CATEGORY(1, 250, 3),

    /** Reveals the subcategory of all cards in the grid. */
    SHOW_SUBCATEGORY(2, 500, 3),

    /** Reveals the series of all cards in the grid. */
    SHOW_SERIES(3, 1000, 3),

    /** Regenerates the grid with cards from a single category. */
    SWIZZLE_CATEGORY(4, 1000, 3),

    //
    // permanent powerups (id starts at 64)

    /** Grants an extra free card flip each day. */
    EXTRA_FLIP(64, 5000, 1);

    /** The cost of this powerup in coins. */
    public final int cost;

    /** The number of charges obtained each time the powerup is purchased. */
    public final int charges;

    /**
     * Returns true if this powerup is permanent, false if it is consumed per use.
     */
    public boolean isPermanent ()
    {
        return _code >= PERMA_CODE;
    }

    // from interface ByteEnum
    public byte toByte ()
    {
        return _code;
    }

    Powerup (int code, int cost, int charges)
    {
        _code = (byte)code;
        this.cost = cost;
        this.charges = charges;
    }

    /** This powerup's byte code. */
    protected byte _code;

    /** Powerups with a code equal to this value or higher are permanent. */
    protected static final int PERMA_CODE = 64;
}
