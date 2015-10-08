//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Represents a card owned by a player.
 */
public class CardRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CardRecord> _R = CardRecord.class;
    public static final ColumnExp<Integer> OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp<Integer> THING_ID = colexp(_R, "thingId");
    public static final ColumnExp<Timestamp> RECEIVED = colexp(_R, "received");
    public static final ColumnExp<Integer> GIVER_ID = colexp(_R, "giverId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 4;

    /** The id of the player that owns this card. */
    @Id public int ownerId;

    /** The thing that's on this card. */
    @Id public int thingId;

    /** The time at which this card was received. */
    @Id public Timestamp received;

    /** The id of the player that gave this card to the owner or 0. */
    public int giverId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link CardRecord}
     * with the supplied key values.
     */
    public static Key<CardRecord> getKey (int ownerId, int thingId, Timestamp received)
    {
        return newKey(_R, ownerId, thingId, received);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(OWNER_ID, THING_ID, RECEIVED); }
    // AUTO-GENERATED: METHODS END
}
