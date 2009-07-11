//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;

/**
 * Contains brief summary info for a thing.
 */
public class ThingCard
    implements IsSerializable, Comparable<ThingCard>
{
    /** The id of the thing represented by this card. */
    public int thingId;

    /** The name of the thing on this card. */
    public String name;

    /** The image of the thing on this card. */
    public String image;

    /** The rarity of the thing on this card. */
    public Rarity rarity;

    /** The time this card was created (not always available). */
    public long created;

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
        card.created = other.created;
        return card;
    }

    // from interface Comparable<ThingCard>
    public int compareTo (ThingCard other)
    {
        int rv = rarity.compareTo(other.rarity);
        if (rv != 0) return rv;
        rv = name.compareTo(other.name);
        if (rv != 0) return rv;
        return (created < other.created) ? -1 : 1;
    }
}
