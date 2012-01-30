//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Tracks whether a player has received a particular attractor card.
 */
public class AttractorGrantedRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AttractorGrantedRecord> _R = AttractorGrantedRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Integer> THING_ID = colexp(_R, "thingId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The id of the user in question. */
    @Id public int userId;

    /** The id of the thing in question. */
    @Id public int thingId;

    /**
     * Mister Constructor.
     */
    public AttractorGrantedRecord (int userId, int thingId)
    {
        this.userId = userId;
        this.thingId = thingId;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AttractorGrantedRecord}
     * with the supplied key values.
     */
    public static Key<AttractorGrantedRecord> getKey (int userId, int thingId)
    {
        return newKey(_R, userId, thingId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID, THING_ID); }
    // AUTO-GENERATED: METHODS END
}
