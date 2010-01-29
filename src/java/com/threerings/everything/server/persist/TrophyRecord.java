//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Tracks multi-set trophies that may be earned.
 */
public class TrophyRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<TrophyRecord> _R = TrophyRecord.class;
    public static final ColumnExp USER_ID = colexp(_R, "userId");
    public static final ColumnExp TROPHY_ID = colexp(_R, "trophyId");
    public static final ColumnExp WHEN = colexp(_R, "when");
    // AUTO-GENERATED: FIELDS END

    /** Increment when changes are made. */
    public static final int SCHEMA_VERSION = 1;

    /** The user that has earned this trophy. */
    @Id public int userId;

    /** The trophy identifier. Will be used to pick the trophy image. */
    @Id public String trophyId;

    /** The time at which the user earned this trophy. */
    public Timestamp when;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link TrophyRecord}
     * with the supplied key values.
     */
    public static Key<TrophyRecord> getKey (int userId, String trophyId)
    {
        return new Key<TrophyRecord>(
                TrophyRecord.class,
                new ColumnExp[] { USER_ID, TROPHY_ID },
                new Comparable[] { userId, trophyId });
    }
    // AUTO-GENERATED: METHODS END
}
