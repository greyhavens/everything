//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import com.samskivert.util.ByteEnum;

/**
 * Defines our different powerup types.
 */
public enum Powerup implements ByteEnum
{
    /** The non-powerup. */
    NOOP(0, 0, 0),

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

    /** Generates grid that contains only cards not held by the player. */
    ALL_NEW_CARDS(4, 750, 3),

    /** Generates grid that contains only cards in series being collected. */
    ALL_COLLECTED_SERIES(5, 250, 3),

    /** Generates grid that contains at least one rarity VII card. */
    ENSURE_ONE_VII(6, 250, 3) {
        public Rarity getBonusRarity () {
            return Rarity.VII;
        }
    },

    /** Generates grid that contains at least one rarity VIII card. */
    ENSURE_ONE_VIII(7, 500, 3) {
        public Rarity getBonusRarity () {
            return Rarity.VIII;
        }
    },

    /** Generates grid that contains at least one rarity IX card. */
    ENSURE_ONE_IX(8, 750, 3) {
        public Rarity getBonusRarity () {
            return Rarity.IX;
        }
    },

    /** Generates grid that contains at least one rarity X card. */
    ENSURE_ONE_X(9, 1000, 3) {
        public Rarity getBonusRarity () {
            return Rarity.X;
        }
    },

    // TODO: all cards from category X?

    //
    // permanent powerups (id starts at 64)

    /** Grants an extra free card flip each day. */
    EXTRA_FLIP(64, 3000, 1) {
        public Player.Flag getTargetFlag () {
            return Player.Flag.EXTRA_FLIP;
        }
    };

    /** Those powerups that can be used during the grid creation process. */
    public static Powerup[] PRE_GRID = new Powerup[] {
        ALL_NEW_CARDS, ALL_COLLECTED_SERIES, ENSURE_ONE_VII, ENSURE_ONE_VIII, ENSURE_ONE_IX,
        ENSURE_ONE_X
    };

    /** Those powerups that can be used on a realized grid. */
    public static Powerup[] POST_GRID = new Powerup[] {
        SHOW_CATEGORY, SHOW_SUBCATEGORY, SHOW_SERIES
    };

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

    /**
     * Returns the rarity of the bonus card placed into a grid created with this powerup. If null,
     * the default is used (anything V or higher).
     */
    public Rarity getBonusRarity ()
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
