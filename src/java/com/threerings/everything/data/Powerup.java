//
// $Id$

package com.threerings.everything.data;

import java.util.EnumSet;

import com.samskivert.depot.ByteEnum;

/**
 * Defines our different powerup types.
 */
public enum Powerup implements ByteEnum
{
    //
    // consumable powerups (id starts at 1)

    /** Reveals the category of all cards in the grid. */
    SHOW_CATEGORY(1, 150, 3) {
        public GridStatus getTargetStatus () {
            return GridStatus.CAT_REVEALED;
        }
    },

    /** Reveals the subcategory of all cards in the grid. */
    SHOW_SUBCATEGORY(2, 300, 3) {
        public GridStatus getTargetStatus () {
            return GridStatus.SUBCAT_REVEALED;
        }
    },

    /** Reveals the series of all cards in the grid. */
    SHOW_SERIES(3, 500, 3) {
        public GridStatus getTargetStatus () {
            return GridStatus.SERIES_REVEALED;
        }
    },

    //
    // permanent powerups (id starts at 64)

    /** Grants an extra free card flip each day. */
    EXTRA_FLIP(64, 3000, 1) {
        public Player.Flag getTargetFlag () {
            return Player.Flag.FREE_FLIP;
        }
    };

    /** Those powerups that can be used during the grid creation process. */
    // public static EnumSet<Powerup> PRE_GRID = EnumSet.of(TODO);

    /** Those powerups that can be used on a realized grid. */
    public static EnumSet<Powerup> POST_GRID =
        EnumSet.of(SHOW_CATEGORY, SHOW_SUBCATEGORY, SHOW_SERIES);

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

    /**
     * Returns the status to which this powerup changes the grid, or null if it is not a status
     * changing powerup.
     */
    public GridStatus getTargetStatus ()
    {
        return null;
    }

    /**
     * Returns the flag that is activated on the player when this powerup is purchased, or null if
     * this is not a flag activating powerup.
     */
    public Player.Flag getTargetFlag ()
    {
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
        this.cost = cost;
        this.charges = charges;
    }

    /** This powerup's byte code. */
    protected byte _code;

    /** Powerups with a code equal to this value or higher are permanent. */
    protected static final int PERMA_CODE = 64;
}
