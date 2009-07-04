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
    /** A unique identifier for this thing. */
    public int thingId;

    /** The set to which this thing belongs. */
    public int setId;

    /** The name of this thing. */
    public String name;

    /** This thing's rarity level (0-9). */
    public byte rarity;

    /** The name of this thing's image (its media hash). */
    public String image;

    /** This thing's description. */
    public String descrip;

    /** Facts about this thing. */
    public String facts;
}
