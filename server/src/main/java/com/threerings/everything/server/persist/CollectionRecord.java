//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server.persist;

import java.util.Date;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.util.RuntimeUtil;

import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.PlayerStats;

/**
 * Contains a summary of a player's collection. Updated periodically.
 */
public class CollectionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CollectionRecord> _R = CollectionRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Integer> THINGS = colexp(_R, "things");
    public static final ColumnExp<Integer> SERIES = colexp(_R, "series");
    public static final ColumnExp<Integer> COMPLETE_SERIES = colexp(_R, "completeSeries");
    public static final ColumnExp<Integer> GIFTS = colexp(_R, "gifts");
    public static final ColumnExp<Boolean> NEEDS_UPDATE = colexp(_R, "needsUpdate");
    // AUTO-GENERATED: FIELDS END

    /** A function for converting this record to a {@link PlayerStats}. */
    public static Function<CollectionRecord, PlayerStats> TO_STATS =
        RuntimeUtil.makeToRuntime(CollectionRecord.class, PlayerStats.class);

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

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

    /** Initializes the {@link PlayerStats#name} field. */
    public PlayerName getName ()
    {
        return PlayerName.create(userId); // caller will fill in the rest
    }

    /** Initializes the {@link PlayerStats#lastSession} field. */
    public Date getLastSession ()
    {
        return null; // caller will fill in the rest
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link CollectionRecord}
     * with the supplied key values.
     */
    public static Key<CollectionRecord> getKey (int userId)
    {
        return newKey(_R, userId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID); }
    // AUTO-GENERATED: METHODS END
}
