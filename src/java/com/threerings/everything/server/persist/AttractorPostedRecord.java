//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

public class AttractorPostedRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AttractorPostedRecord> _R = AttractorPostedRecord.class;
    public static final ColumnExp USER_ID = colexp(_R, "userId");
    public static final ColumnExp THING_ID = colexp(_R, "thingId");
    public static final ColumnExp WHEN = colexp(_R, "when");
    // AUTO-GENERATED: FIELDS END

    /** Increment on schema changes. */
    public static final int SCHEMA_VERSION = 1;

    /** The user that posted the attractor. */
    @Id public int userId;

    /** The thing that was posted. */
    @Id public int thingId;

    /** The timestamp. */
    public Timestamp when;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AttractorPostedRecord}
     * with the supplied key values.
     */
    public static Key<AttractorPostedRecord> getKey (int userId, int thingId)
    {
        return new Key<AttractorPostedRecord>(
                AttractorPostedRecord.class,
                new ColumnExp[] { USER_ID, THING_ID },
                new Comparable[] { userId, thingId });
    }
    // AUTO-GENERATED: METHODS END
}
