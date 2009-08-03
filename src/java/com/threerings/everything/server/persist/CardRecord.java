//
// $Id$

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
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp THING_ID = colexp(_R, "thingId");
    public static final ColumnExp RECEIVED = colexp(_R, "received");
    public static final ColumnExp GIVER_ID = colexp(_R, "giverId");
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
        return new Key<CardRecord>(
                CardRecord.class,
                new ColumnExp[] { OWNER_ID, THING_ID, RECEIVED },
                new Comparable[] { ownerId, thingId, received });
    }
    // AUTO-GENERATED: METHODS END
}
