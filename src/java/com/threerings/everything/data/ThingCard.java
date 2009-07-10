//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;

/**
 * Contains brief summary info for a thing.
 */
public class ThingCard
    implements IsSerializable
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
}
