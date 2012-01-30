//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server.persist;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Used to load information on a player's owned series.
 */
@Computed(shadowOf=CategoryRecord.class)
public class OwnedRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<OwnedRecord> _R = OwnedRecord.class;
    public static final ColumnExp<Integer> CATEGORY_ID = colexp(_R, "categoryId");
    public static final ColumnExp<Integer> OWNED = colexp(_R, "owned");
    // AUTO-GENERATED: FIELDS END

    /** The category id of the series in question. */
    public int categoryId;

    /** The number of cards owned in this series. */
    public int owned;
}
