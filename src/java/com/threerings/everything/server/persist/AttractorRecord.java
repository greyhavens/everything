//
// $Id$

package com.threerings.everything.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.util.RuntimeUtil;

import com.google.common.base.Function;

import com.threerings.everything.data.Thing;

import com.threerings.everything.server.AttractorInfo;

/**
 * Contains cute upselly text for particularly cool things in our database.
 *
 * TODO: remove. No longer used.
 */
public class AttractorRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AttractorRecord> _R = AttractorRecord.class;
    public static final ColumnExp THING_ID = colexp(_R, "thingId");
    public static final ColumnExp TITLE = colexp(_R, "title");
    public static final ColumnExp MESSAGE = colexp(_R, "message");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The thingId that we're representing. */
    @Id
    public int thingId;

    /** The fun title to use for feed posts. */
    @Column(length=Thing.MAX_NAME_LENGTH)
    public String title;

    /** The enticing text of the feed post. */
    @Column(length=Thing.MAX_FACTS_LENGTH)
    public String message;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AttractorRecord}
     * with the supplied key values.
     */
    public static Key<AttractorRecord> getKey (int thingId)
    {
        return new Key<AttractorRecord>(
                AttractorRecord.class,
                new ColumnExp[] { THING_ID },
                new Comparable[] { thingId });
    }
    // AUTO-GENERATED: METHODS END

    /** A function for converting this record to an {@link AttractorInfo}. */
    public static Function<AttractorRecord, AttractorInfo> TO_INFO =
        RuntimeUtil.makeToRuntime(AttractorRecord.class, AttractorInfo.class);

    /**
     * Turns this into an AttractorInfo.
     */
    public AttractorInfo toInfo ()
    {
        return TO_INFO.apply(this);
    }
}
