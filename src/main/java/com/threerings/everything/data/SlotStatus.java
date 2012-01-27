//
// $Id$

package com.threerings.everything.data;

import com.samskivert.util.ByteEnum;

/**
 * Used to track the status of each slot in the grid.
 */
public enum SlotStatus implements ByteEnum
{
    /** The card at this slot is unflipped. */
    UNFLIPPED(0),

    /** The card at this slot has been flipped. */
    FLIPPED(1),

    /** The card at this slot has been gifted. */
    GIFTED(2),

    /** The card at this slot has been sold. */
    SOLD(3),

    /** The card was recruitment gifted (runtime only, nongrid). */
    RECRUIT_GIFTED(4);

    // from interface ByteEnum
    public byte toByte () {
        return _code;
    }

    SlotStatus (int code) {
        _code = (byte)code;
    }

    protected byte _code;
}
