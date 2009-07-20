//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Provides data for a thing to the client.
 */
public class Thing
    implements Created, IsSerializable, Comparable<Thing>
{
    /** The maximum length of a thing name. */
    public static final int MAX_NAME_LENGTH = 64;

    /** The maximum length of a thing description. */
    public static final int MAX_DESCRIP_LENGTH = 255;

    /** The maximum length of thing facts. */
    public static final int MAX_FACTS_LENGTH = 2048;

    /** The largest allowed width for a thing image. */
    public static final int MAX_IMAGE_WIDTH = 250;

    /** The largest allowed height for a thing image. */
    public static final int MAX_IMAGE_HEIGHT = 300;

    /** A unique identifier for this thing. */
    public int thingId;

    /** The (leaf) category to which this thing belongs. */
    public int categoryId;

    /** The name of this thing. */
    public String name;

    /** This thing's rarity level. */
    public Rarity rarity;

    /** The name of this thing's image (its media hash). */
    public String image;

    /** This thing's description. */
    public String descrip;

    /** Facts about this thing. */
    public String facts;

    /** The URL from which this thing's information was obtained. */
    public String source;

    /** The creator of this thing. */
    public PlayerName creator;

    /** Converts this Thing to a ThingCard. */
    public ThingCard toCard ()
    {
        ThingCard card = new ThingCard();
        card.thingId = thingId;
        card.name = name;
        card.image = image;
        card.rarity = rarity;
        return card;
    }

    // from interface Created
    public int getCreatorId ()
    {
        return creator.userId;
    }

    // from interface Comparable<Thing>
    public int compareTo (Thing other)
    {
        return (rarity != other.rarity) ? rarity.compareTo(other.rarity) :
            name.compareTo(other.name);
    }
}
