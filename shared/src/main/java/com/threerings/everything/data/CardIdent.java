//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.data;

import java.io.Serializable;

/**
 * Used to identify a card.
 */
public class CardIdent
    implements Serializable
{
    /** The owner of the card. */
    public int ownerId;

    /** The thing on the card. */
    public int thingId;

    /** The time at which the card was received. */
    public long received;

    /** Used when unserializing. */
    public CardIdent () {
    }

    /**
     * Creates an id for the specified card.
     */
    public CardIdent (int ownerId, int thingId, long received) {
        this.ownerId = ownerId;
        this.thingId = thingId;
        this.received = received;
    }
}
