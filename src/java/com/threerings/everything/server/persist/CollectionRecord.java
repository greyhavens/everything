//
// $Id$

package com.threerings.everything.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Contains a summary of a player's collection. Updated periodically.
 */
public class CollectionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CollectionRecord> _R = CollectionRecord.class;
    public static final ColumnExp USER_ID = colexp(_R, "userId");
    public static final ColumnExp THINGS = colexp(_R, "things");
    public static final ColumnExp SERIES = colexp(_R, "series");
    public static final ColumnExp COMPLETE_SERIES = colexp(_R, "completeSeries");
    public static final ColumnExp GIFTS = colexp(_R, "gifts");
    public static final ColumnExp NEEDS_UPDATE = colexp(_R, "needsUpdate");
    // AUTO-GENERATED: FIELDS END

    /** The id of the user whose collection is summarized. */
    @Id public int userId;

    /** The total number of (unique) things in this user's collection. */
    public int things;

    /** The total number of series in which this user has at least one card. */
    public int series;

    /** The total number of complete series this user has. */
    public int completeSeries;

    /** The number of times this player has gifted cards. */
    public int gifts;

    /** Marks this collection record as needing to be recomputed. */
    public boolean needsUpdate;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link CollectionRecord}
     * with the supplied key values.
     */
    public static Key<CollectionRecord> getKey (int userId)
    {
        return new Key<CollectionRecord>(
                CollectionRecord.class,
                new ColumnExp[] { USER_ID },
                new Comparable[] { userId });
    }
    // AUTO-GENERATED: METHODS END
}
