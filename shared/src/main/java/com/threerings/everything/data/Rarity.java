//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.util.EnumSet;

import com.samskivert.util.ByteEnum;

/**
 * Defines the various rarities of things.
 */
public enum Rarity implements ByteEnum
{
    I(0, 10, "Grows on trees"),
    II(1, 20, "Very common"),
    III(2, 35, "Common"),
    IV(3, 55, "Fairly common"),
    V(4, 90, "Uncommon"),
    VI(5, 145, "Fairly rare"),
    VII(6, 235, "Rare"),
    VIII(7, 380, "Very rare"),
    IX(8, 615, "Ultra rare"),
    X(9, 995, "Mythical");

    /** Cards considered rare enough to be exciting. */
    public static final EnumSet<Rarity> BONUS = EnumSet.of(V, VI, VII, VIII, IX, X);

    /** The minimum rarity of a thing given on your birthday. */
    public static final Rarity MIN_GIFT_RARITY = IX;

    /** The coin value of a card of this rarity. */
    public final int value;

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
        return _weight;
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

    /** The weight of this Rarity (computed after all instances are constructed) */
    protected int _weight;

    protected static int computeRarity (Rarity rarity)
    {
        float sumWeights = 0;
        for (Rarity r : values()) {
            sumWeights += r.weight();
        }
        return Math.round(sumWeights / rarity.weight());
    }

    static { // initialize the _weight value of each Rarity
        int totalCost = 0;
        for (Rarity r : values()) {
            totalCost += r.value;
        }
        float avgCost100 = 100f * totalCost / values().length;
        for (Rarity r : values()) {
            r._weight = Math.round(avgCost100 / r.value);
        }
    }
}
