//
// $Id$

package com.threerings.everything.data;

import com.samskivert.depot.ByteEnum;

/**
 * Defines the various rarities of things.
 */
public enum Rarity implements ByteEnum
{
    I(0, 10),   II(1, 20),   III(2, 35),   IV(3, 55),  V(4, 90),
    VI(5, 145), VII(6, 235), VIII(7, 380), IX(8, 615), X(9, 995);

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

    public int weight ()
    {
        return MAX_VALUE - value + EXTRA_WEIGHT;
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

    protected static int computeMaxValue ()
    {
        int maxValue = 0;
        for (Rarity rarity : Rarity.values()) {
            maxValue = Math.max(maxValue, rarity.value);
        }
        return maxValue;
    }

    /** The maximum value of any rarity. */
    protected static final int MAX_VALUE = computeMaxValue();

    /** Extra weight added to every card to make the rarest card not insanely rare. */
    protected static final int EXTRA_WEIGHT = 50;
}
