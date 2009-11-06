//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;

import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Records generation and use of a player's daily recruitment gift.
 */
public class RecruitGiftRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<RecruitGiftRecord> _R = RecruitGiftRecord.class;
    public static final ColumnExp USER_ID = colexp(_R, "userId");
    public static final ColumnExp LAST_GENERATED = colexp(_R, "lastGenerated");
    public static final ColumnExp GIFT_ID = colexp(_R, "giftId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
      * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The Samsara user id of this player. */
    @Id public int userId;

    /** The time at which this record expires. */
    public Timestamp lastGenerated; //expires;

    /** The thingId of the gift, or 0 if it's already been gifted. */
    public int giftId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link RecruitGiftRecord}
     * with the supplied key values.
     */
    public static Key<RecruitGiftRecord> getKey (int userId)
    {
        return new Key<RecruitGiftRecord>(
                RecruitGiftRecord.class,
                new ColumnExp[] { USER_ID },
                new Comparable[] { userId });
    }
    // AUTO-GENERATED: METHODS END
}
