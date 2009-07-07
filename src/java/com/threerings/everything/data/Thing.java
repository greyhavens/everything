//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Provides data for a thing to the client.
 */
public class Thing
    implements IsSerializable
{
    /** The maximum length of a thing name. */
    public static final int MAX_NAME_LENGTH = 64;

    /** The maximum length of a thing description. */
    public static final int MAX_DESCRIP_LENGTH = 255;

    /** The maximum length of thing facts. */
    public static final int MAX_FACTS_LENGTH = 2048;

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
}
