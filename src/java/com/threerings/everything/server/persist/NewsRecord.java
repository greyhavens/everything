//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.util.RuntimeUtil;

import com.threerings.everything.data.News;
import com.threerings.everything.data.PlayerName;

/**
 * Used to display news on the landing page.
 */
public class NewsRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<NewsRecord> _R = NewsRecord.class;
    public static final ColumnExp REPORTED = colexp(_R, "reported");
    public static final ColumnExp REPORTER_ID = colexp(_R, "reporterId");
    public static final ColumnExp TEXT = colexp(_R, "text");
    // AUTO-GENERATED: FIELDS END

    /** A function for converting persistent records to runtime records. */
    public static Function<NewsRecord, News> TO_NEWS =
        RuntimeUtil.makeToRuntime(NewsRecord.class, News.class);

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The time at which this news was reported. */
    @Id public Timestamp reported;

    /** The admin that added this news. */
    public int reporterId;

    /** The text of the news post. */
    @Column(length=News.MAX_NEWS_LENGTH)
    public String text;

    /**
     * Initializes {@link News#reporter}.
     */
    public PlayerName getReporter ()
    {
        return PlayerName.create(reporterId);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link NewsRecord}
     * with the supplied key values.
     */
    public static Key<NewsRecord> getKey (Timestamp reported)
    {
        return new Key<NewsRecord>(
                NewsRecord.class,
                new ColumnExp[] { REPORTED },
                new Comparable[] { reported });
    }
    // AUTO-GENERATED: METHODS END
}
