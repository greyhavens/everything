//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Date;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Used to summarize user registrations.
 */
@Computed(shadowOf=PlayerRecord.class)
public class RegiSummaryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<RegiSummaryRecord> _R = RegiSummaryRecord.class;
    public static final ColumnExp WHEN = colexp(_R, "when");
    public static final ColumnExp COUNT = colexp(_R, "count");
    // AUTO-GENERATED: FIELDS END

    /** The date for which we're summarizing. */
    @Computed(fieldDefinition="date(\"joined\")")
    public Date when;

    /** The number of players that joined on this date. */
    @Computed(fieldDefinition="count(*)")
    public int count;
}
