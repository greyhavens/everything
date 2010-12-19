//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Tracks when a player has completed a series.
 */
public class SeriesRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SeriesRecord> _R = SeriesRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Integer> CATEGORY_ID = colexp(_R, "categoryId");
    public static final ColumnExp<Timestamp> WHEN = colexp(_R, "when");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The player that has completed the series in question. */
    @Id public int userId;

    /** The category id of the completed series. */
    @Id public int categoryId;

    /** The time at which the player completed the series. */
    public Timestamp when;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SeriesRecord}
     * with the supplied key values.
     */
    public static Key<SeriesRecord> getKey (int userId, int categoryId)
    {
        return newKey(_R, userId, categoryId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID, CATEGORY_ID); }
    // AUTO-GENERATED: METHODS END
}
