//
// $Id$

package com.threerings.everything.data;

import com.samskivert.depot.ByteEnum;

/**
 * Tracks status attributes of a grid.
 */
public enum GridStatus implements ByteEnum
{
    /** The grid is normal. */
    NORMAL(0),

    /** The grid has had its category revealed. */
    CAT_REVEALED(1),

    /** The grid has had its subcategory revealed. */
    SUBCAT_REVEALED(2),

    /** The grid has had its series revealed. */
    SERIES_REVEALED(3);

    // from interface ByteEnum
    public byte toByte ()
    {
        return _code;
    }

    GridStatus (int code) {
        _code = (byte)code;
    }

    /** This rarity's byte code. */
    protected byte _code;
}