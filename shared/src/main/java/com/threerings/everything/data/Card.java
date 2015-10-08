//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.data;

import java.io.Serializable;

/**
 * Provides data for a card to the client.
 */
public class Card
    implements Serializable
{
    /** A special id indicating that a card was received as a birthday present. */
    public static final int BIRTHDAY_GIVER_ID = -1;

    /** The player that owns this card. */
    public PlayerName owner;

    /** The categories of this card. */
    public Category[] categories;

    /** The thing that's on this card. */
    public Thing thing;

    /** This thing's position in its series (counting from zero). */
    public int position;

    /** The number of things in this thing's series. */
    public int things;

    /** The time at which this card was received. */
    public long received;

    /** The player that gave this card to the owner or null. */
    public PlayerName giver;

    /** Returns the series of the thing on this card. */
    public Category getSeries ()
    {
        return categories[categories.length-1];
    }

    /** Returns an identifier for this card. */
    public CardIdent getIdent ()
    {
        return new CardIdent(owner.userId, thing.thingId, received);
    }

    /** Converts this Card to a ThingCard. */
    public ThingCard toThingCard ()
    {
        return thing.toCard(received);
    }
}
