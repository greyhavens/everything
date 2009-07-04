//
// $Id$

package com.threerings.everything.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Contains information on a particular set.
 */
public class SetRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SetRecord> _R = SetRecord.class;
    public static final ColumnExp SET_ID = colexp(_R, "setId");
    public static final ColumnExp CATEGORY_ID = colexp(_R, "categoryId");
    // AUTO-GENERATED: FIELDS END

    /** A unique identifier for this set. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int setId;

    /** The category to which this set belongs. */
    @Index public int categoryId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SetRecord}
     * with the supplied key values.
     */
    public static Key<SetRecord> getKey (int setId)
    {
        return new Key<SetRecord>(
                SetRecord.class,
                new ColumnExp[] { SET_ID },
                new Comparable[] { setId });
    }
    // AUTO-GENERATED: METHODS END
}
