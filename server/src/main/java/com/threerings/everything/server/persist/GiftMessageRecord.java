//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Used to store a message sent along with a gift for a gifted card.
 */
public class GiftMessageRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GiftMessageRecord> _R = GiftMessageRecord.class;
    public static final ColumnExp<Integer> OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp<Integer> THING_ID = colexp(_R, "thingId");
    public static final ColumnExp<Long> RECEIVED = colexp(_R, "received");
    public static final ColumnExp<String> MESSAGE = colexp(_R, "message");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The id of the recipient of this message. */
    @Id public int ownerId;

    /** The id of the thing that was gifted. */
    @Id public int thingId;

    /** The timestamp identifying the gift. */
    @Id public long received;

    /** The gift message. */
    public String message;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GiftMessageRecord}
     * with the supplied key values.
     */
    public static Key<GiftMessageRecord> getKey (int ownerId, int thingId, long received)
    {
        return newKey(_R, ownerId, thingId, received);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(OWNER_ID, THING_ID, RECEIVED); }
    // AUTO-GENERATED: METHODS END
}
