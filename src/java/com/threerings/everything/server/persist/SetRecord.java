//
// $Id$

package com.threerings.everything.server.persist;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.util.RuntimeUtil;

import com.threerings.everything.data.ThingSet;

/**
 * Contains information on a particular set.
 */
public class SetRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SetRecord> _R = SetRecord.class;
    public static final ColumnExp SET_ID = colexp(_R, "setId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp CATEGORY_ID = colexp(_R, "categoryId");
    public static final ColumnExp CREATOR_ID = colexp(_R, "creatorId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** A function for converting persistent records to runtime records. */
    public static Function<SetRecord, ThingSet> TO_SET =
        RuntimeUtil.makeToRuntime(SetRecord.class, ThingSet.class);

    /** A function for converting runtime records to persistent records. */
    public static Function<ThingSet, SetRecord> FROM_SET =
        RuntimeUtil.makeToRecord(ThingSet.class, SetRecord.class);

    /** A unique identifier for this set. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int setId;

    @Column(length=ThingSet.MAX_NAME_LENGTH)
    public String name;

    /** The category to which this set belongs. */
    @Index public int categoryId;

    /** The id of the user that created this set. */
    public int creatorId;

    /**
     * Converts this persistent record to a runtime record.
     */
    public ThingSet toSet ()
    {
        return TO_SET.apply(this);
    }

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
