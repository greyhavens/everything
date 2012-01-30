//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Tracks votes by editors for pending series.
 */
public class PendingVoteRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<PendingVoteRecord> _R = PendingVoteRecord.class;
    public static final ColumnExp<Integer> CATEGORY_ID = colexp(_R, "categoryId");
    public static final ColumnExp<Integer> VOTER_ID = colexp(_R, "voterId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The id of the category for which a vote was counted. */
    @Id public int categoryId;

    /** The user id of the voter. */
    @Id public int voterId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link PendingVoteRecord}
     * with the supplied key values.
     */
    public static Key<PendingVoteRecord> getKey (int categoryId, int voterId)
    {
        return newKey(_R, categoryId, voterId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(CATEGORY_ID, VOTER_ID); }
    // AUTO-GENERATED: METHODS END
}
