//
// $Id$

package com.threerings.everything.data;

/**
 * Defines the various rarities of things.
 */
public enum Rarity
{
    I(0, 10),   II(1, 15),   III(2, 35),   IV(3, 50),  V(4, 85),
    VI(5, 135), VII(6, 220), VIII(7, 355), IX(8, 575), X(9, 930);

    /** The coin value of a card of this rarity. */
    public final int value;

    /**
     * Returns the {@link Rarity} associated with the supplied code or null.
     */
    public static Rarity fromByte (byte code)
    {
        for (Rarity value : Rarity.values()) {
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

    Rarity (int code, int value) {
        this.value = value;
        _code = (byte)code;
    }

    /** This rarity's byte code. */
    protected byte _code;
}
