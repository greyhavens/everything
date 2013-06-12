//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.io.Serializable;

/**
 * Contains brief summary info for a thing.
 */
public class ThingCard
    implements Serializable, Comparable<ThingCard>
{
    /** The id of the thing represented by this card. */
    public int thingId;

    /** The name of the thing on this card. */
    public String name;

    /** The image of the thing on this card. */
    public String image;

    /** The rarity of the thing on this card. */
    public Rarity rarity;

    /** The time this card was received (not always available). */
    public long received;

    /**
     * Clones the supplied thing card. We can't use {@link Object#clone} because GWT needs to grok
     * this class, even though we'd only ever call clone() on the server. Sigh.
     */
    public static ThingCard clone (ThingCard other)
    {
        ThingCard card = new ThingCard();
        card.thingId = other.thingId;
        card.name = other.name;
        card.image = other.image;
        card.rarity = other.rarity;
        card.received = other.received;
        return card;
    }

    /**
     * Creates a partially revealed card (which has only a name).
     */
    public static ThingCard newPartial (String name)
    {
        ThingCard card = new ThingCard();
        card.name = name;
        return card;
    }

    // from interface Comparable<ThingCard>
    public int compareTo (ThingCard other)
    {
        int rv = rarity.compareTo(other.rarity);
        if (rv != 0) return rv;
        rv = name.compareTo(other.name);
        if (rv != 0) return rv;
        return (received < other.received) ? -1 : 1;
    }
}
