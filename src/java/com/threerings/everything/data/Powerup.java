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

    /**
     * Returns the {@link Powerup} associated with the supplied code or null.
     */
    public static Powerup fromByte (byte code)
    {
        for (Powerup value : Powerup.values()) {
            if (value.toByte() == code) {
                return value;
            }
        }
        return null;
    }

    // from interface ByteEnum
    public byte toByte ()
    {
        return _code;
    }

    Powerup (int code, int cost, int charges)
    {
        _code = (byte)code;
        _cost = cost;
        _charges = charges;
    }

    /** This powerup's byte code. */
    protected byte _code;

    /** The cost of this powerup in coins. */
    protected int _cost;

    /** The number of charges obtained each time the powerup is purchased. */
    protected int _charges;
}
