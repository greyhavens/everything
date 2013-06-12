//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;

import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.util.ArrayUtil;

/**
 * Records generation and use of a player's daily recruitment gift.
 */
public class RecruitGiftRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<RecruitGiftRecord> _R = RecruitGiftRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Timestamp> EXPIRES = colexp(_R, "expires");
    public static final ColumnExp<int[]> GIFT_IDS = colexp(_R, "giftIds");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
      * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 4;

    /** The Samsara user id of this player. */
    @Id public int userId;

    /** The time at which this record expires. */
    public Timestamp expires;

    /** The thingIds of each potential gift, or 0 to indicate "gifted". */
    public int[] giftIds;

    /**
     * Get the index of the specified gift, or -1.
     */
    public int getGiftIndex (int thingId)
    {
        return ArrayUtil.indexOf(giftIds, thingId);
    }

    /**
     * Return true if there is a nonzero number of gifts here and all are ungifted.
     */
    public boolean isUnused ()
    {
        for (int gift : giftIds) {
            if (gift == 0) {
                return false;
            }
        }
        return (giftIds.length > 0);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link RecruitGiftRecord}
     * with the supplied key values.
     */
    public static Key<RecruitGiftRecord> getKey (int userId)
    {
        return newKey(_R, userId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID); }
    // AUTO-GENERATED: METHODS END
}
