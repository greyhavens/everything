//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Does something extraordinary.
 */
public class EscrowedCardRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<EscrowedCardRecord> _R = EscrowedCardRecord.class;
    public static final ColumnExp EXTERNAL_ID = colexp(_R, "externalId");
    public static final ColumnExp THING_ID = colexp(_R, "thingId");
    public static final ColumnExp CREATED = colexp(_R, "created");
    public static final ColumnExp ESCROWED = colexp(_R, "escrowed");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The player's external site id. */
    @Id public String externalId;

    /** The thing on the escrowed card. */
    @Id public int thingId;

    /** The time at which the escrowed card was created. */
    @Id public Timestamp created;

    /** The time at which this card was escrowed. */
    public Timestamp escrowed;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link EscrowedCardRecord}
     * with the supplied key values.
     */
    public static Key<EscrowedCardRecord> getKey (String externalId, int thingId, Timestamp created)
    {
        return new Key<EscrowedCardRecord>(
                EscrowedCardRecord.class,
                new ColumnExp[] { EXTERNAL_ID, THING_ID, CREATED },
                new Comparable[] { externalId, thingId, created });
    }
    // AUTO-GENERATED: METHODS END
}
