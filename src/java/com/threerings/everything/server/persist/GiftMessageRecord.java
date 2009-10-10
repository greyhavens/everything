//
// $Id$

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
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp THING_ID = colexp(_R, "thingId");
    public static final ColumnExp RECEIVED = colexp(_R, "received");
    public static final ColumnExp MESSAGE = colexp(_R, "message");
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
        return new Key<GiftMessageRecord>(
                GiftMessageRecord.class,
                new ColumnExp[] { OWNER_ID, THING_ID, RECEIVED },
                new Comparable[] { ownerId, thingId, received });
    }
    // AUTO-GENERATED: METHODS END
}