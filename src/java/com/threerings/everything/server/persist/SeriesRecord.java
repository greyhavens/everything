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

import com.threerings.everything.data.Series;

/**
 * Contains information on a particular series.
 */
public class SeriesRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SeriesRecord> _R = SeriesRecord.class;
    public static final ColumnExp SERIES_ID = colexp(_R, "seriesId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp CATEGORY_ID = colexp(_R, "categoryId");
    public static final ColumnExp CREATOR_ID = colexp(_R, "creatorId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** A function for converting persistent records to runtime records. */
    public static Function<SeriesRecord, Series> TO_SERIES =
        RuntimeUtil.makeToRuntime(SeriesRecord.class, Series.class);

    /** A function for converting runtime records to persistent records. */
    public static Function<Series, SeriesRecord> FROM_SERIES =
        RuntimeUtil.makeToRecord(Series.class, SeriesRecord.class);

    /** A unique identifier for this series. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int seriesId;

    @Column(length=Series.MAX_NAME_LENGTH)
    public String name;

    /** The category to which this series belongs. */
    @Index public int categoryId;

    /** The id of the user that created this series. */
    public int creatorId;

    /**
     * Converts this persistent record to a runtime record.
     */
    public Series toSeries ()
    {
        return TO_SERIES.apply(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SeriesRecord}
     * with the supplied key values.
     */
    public static Key<SeriesRecord> getKey (int seriesId)
    {
        return new Key<SeriesRecord>(
                SeriesRecord.class,
                new ColumnExp[] { SERIES_ID },
                new Comparable[] { seriesId });
    }
    // AUTO-GENERATED: METHODS END
}
