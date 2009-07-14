//
// $Id$

package com.threerings.everything.data;

import com.samskivert.depot.ByteEnum;

/**
 * Defines the various rarities of things.
 */
public enum Rarity implements ByteEnum
{
    I(0, 10, "Ultra common"),
    II(1, 20, "Very common"),
    III(2, 35, "Common"),
    IV(3, 55, "Fairly common"),
    V(4, 90, "Uncommon"),
    VI(5, 145, "Fairly rare"),
    VII(6, 235, "Rare"),
    VIII(7, 380, "Very rare"),
    IX(8, 615, "Ultra rare"),
    X(9, 995, "Mythical");

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

    /**
     * Returns the value obtained when selling a card of this rarity.
     */
    public int saleValue ()
    {
        return value/2;
    }

    /**
     * Returns this rarity's weight for use when building grids of cards.
     */
    public int weight ()
    {
        return Math.round(100 * AVG_COST / value);
    }

    /**
     * Returns a human readable description of this rarity.
     */
    public String description ()
    {
        return _descrip + " (1 in " + computeRarity(this) + ")";
    }

    // from interface ByteEnum
    public byte toByte ()
    {
        return _code;
    }

    Rarity (int code, int value, String descrip) {
        this.value = value;
        _descrip = descrip;
        _code = (byte)code;
    }

    /** This rarity's byte code. */
    protected byte _code;

    /** A human grokkable description of this rarity. */
    protected String _descrip;

    protected static float computeAvgCost ()
    {
        float totalCost = 0;
        int count = 0;
        for (Rarity rarity : Rarity.values()) {
            totalCost += rarity.value;
            count++;
        }
        return totalCost / count;
    }

    protected static int computeRarity (Rarity rarity)
    {
        float sumWeights = 0;
        for (Rarity r : Rarity.values()) {
            sumWeights += r.weight();
        }
        return Math.round(sumWeights / rarity.weight());
    }

    /** The maximum value of any rarity. */
    protected static final float AVG_COST = computeAvgCost();

    /** Extra weight added to every card to make the rarest card not insanely rare. */
    protected static final int EXTRA_WEIGHT = 30;
}
