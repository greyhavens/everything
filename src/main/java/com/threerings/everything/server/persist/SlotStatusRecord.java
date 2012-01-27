//
// $Id$

package com.threerings.everything.server.persist;

import java.util.Arrays;
import java.util.List;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.everything.data.SlotStatus;

/**
 * Maintains the status for each slot in a player's active grid. We track the status of each slot
 * in a separate column so that we can use the database to prevent funny business if the user tries
 * to flip cards from multiple clients at the same instant.
 */
public class SlotStatusRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SlotStatusRecord> _R = SlotStatusRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<SlotStatus> STATUS0 = colexp(_R, "status0");
    public static final ColumnExp<SlotStatus> STATUS1 = colexp(_R, "status1");
    public static final ColumnExp<SlotStatus> STATUS2 = colexp(_R, "status2");
    public static final ColumnExp<SlotStatus> STATUS3 = colexp(_R, "status3");
    public static final ColumnExp<SlotStatus> STATUS4 = colexp(_R, "status4");
    public static final ColumnExp<SlotStatus> STATUS5 = colexp(_R, "status5");
    public static final ColumnExp<SlotStatus> STATUS6 = colexp(_R, "status6");
    public static final ColumnExp<SlotStatus> STATUS7 = colexp(_R, "status7");
    public static final ColumnExp<SlotStatus> STATUS8 = colexp(_R, "status8");
    public static final ColumnExp<SlotStatus> STATUS9 = colexp(_R, "status9");
    public static final ColumnExp<SlotStatus> STATUS10 = colexp(_R, "status10");
    public static final ColumnExp<SlotStatus> STATUS11 = colexp(_R, "status11");
    public static final ColumnExp<SlotStatus> STATUS12 = colexp(_R, "status12");
    public static final ColumnExp<SlotStatus> STATUS13 = colexp(_R, "status13");
    public static final ColumnExp<SlotStatus> STATUS14 = colexp(_R, "status14");
    public static final ColumnExp<SlotStatus> STATUS15 = colexp(_R, "status15");
    public static final ColumnExp<Long> FLIPPED0 = colexp(_R, "flipped0");
    public static final ColumnExp<Long> FLIPPED1 = colexp(_R, "flipped1");
    public static final ColumnExp<Long> FLIPPED2 = colexp(_R, "flipped2");
    public static final ColumnExp<Long> FLIPPED3 = colexp(_R, "flipped3");
    public static final ColumnExp<Long> FLIPPED4 = colexp(_R, "flipped4");
    public static final ColumnExp<Long> FLIPPED5 = colexp(_R, "flipped5");
    public static final ColumnExp<Long> FLIPPED6 = colexp(_R, "flipped6");
    public static final ColumnExp<Long> FLIPPED7 = colexp(_R, "flipped7");
    public static final ColumnExp<Long> FLIPPED8 = colexp(_R, "flipped8");
    public static final ColumnExp<Long> FLIPPED9 = colexp(_R, "flipped9");
    public static final ColumnExp<Long> FLIPPED10 = colexp(_R, "flipped10");
    public static final ColumnExp<Long> FLIPPED11 = colexp(_R, "flipped11");
    public static final ColumnExp<Long> FLIPPED12 = colexp(_R, "flipped12");
    public static final ColumnExp<Long> FLIPPED13 = colexp(_R, "flipped13");
    public static final ColumnExp<Long> FLIPPED14 = colexp(_R, "flipped14");
    public static final ColumnExp<Long> FLIPPED15 = colexp(_R, "flipped15");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** Our status columns, in grid position order. */
    @SuppressWarnings("unchecked")
    public static List<ColumnExp<SlotStatus>> STATUSES = Arrays.asList(
        STATUS0, STATUS1, STATUS2, STATUS3, STATUS4, STATUS5, STATUS6, STATUS7,
        STATUS8, STATUS9, STATUS10, STATUS11, STATUS12, STATUS13, STATUS14, STATUS15);

    /** Our timestamp columns, in grid position order. */
    @SuppressWarnings("unchecked")
    public static List<ColumnExp<Long>> STAMPS = Arrays.asList(
        FLIPPED0, FLIPPED1, FLIPPED2, FLIPPED3, FLIPPED4, FLIPPED5, FLIPPED6, FLIPPED7,
        FLIPPED8, FLIPPED9, FLIPPED10, FLIPPED11, FLIPPED12, FLIPPED13, FLIPPED14, FLIPPED15);

    /** The user for whom we're tracking grid status. */
    @Id public int userId;

    /** The status of slot 0. */
    public SlotStatus status0;

    /** The status of slot 1. */
    public SlotStatus status1;

    /** The status of slot 2. */
    public SlotStatus status2;

    /** The status of slot 3. */
    public SlotStatus status3;

    /** The status of slot 4. */
    public SlotStatus status4;

    /** The status of slot 5. */
    public SlotStatus status5;

    /** The status of slot 6. */
    public SlotStatus status6;

    /** The status of status 7. */
    public SlotStatus status7;

    /** The status of slot 8. */
    public SlotStatus status8;

    /** The status of slot 9. */
    public SlotStatus status9;

    /** The status of slot 10. */
    public SlotStatus status10;

    /** The status of slot 11. */
    public SlotStatus status11;

    /** The status of slot 12. */
    public SlotStatus status12;

    /** The status of slot 13. */
    public SlotStatus status13;

    /** The status of slot 14. */
    public SlotStatus status14;

    /** The status of slot 15. */
    public SlotStatus status15;

    /** When slot 0 was flipped iff it's UNFLIPPED. */
    public long flipped0;

    /** When slot 1 was flipped iff it's UNFLIPPED. */
    public long flipped1;

    /** When slot 2 was flipped iff it's UNFLIPPED. */
    public long flipped2;

    /** When slot 3 was flipped iff it's UNFLIPPED. */
    public long flipped3;

    /** When slot 4 was flipped iff it's UNFLIPPED. */
    public long flipped4;

    /** When slot 5 was flipped iff it's UNFLIPPED. */
    public long flipped5;

    /** When slot 6 was flipped iff it's UNFLIPPED. */
    public long flipped6;

    /** When slot 7 was flipped iff it's UNFLIPPED. */
    public long flipped7;

    /** When slot 8 was flipped iff it's UNFLIPPED. */
    public long flipped8;

    /** When slot 9 was flipped iff it's UNFLIPPED. */
    public long flipped9;

    /** When slot 10 was flipped iff it's UNFLIPPED. */
    public long flipped10;

    /** When slot 11 was flipped iff it's UNFLIPPED. */
    public long flipped11;

    /** When slot 12 was flipped iff it's UNFLIPPED. */
    public long flipped12;

    /** When slot 13 was flipped iff it's UNFLIPPED. */
    public long flipped13;

    /** When slot 14 was flipped iff it's UNFLIPPED. */
    public long flipped14;

    /** When slot 15 was flipped iff it's UNFLIPPED. */
    public long flipped15;

    /**
     * Returns the status of every column in the grid in array order.
     */
    public SlotStatus[] toStatuses ()
    {
        return new SlotStatus[] {
            status0, status1, status2, status3, status4, status5, status6, status7,
            status8, status9, status10, status11, status12, status13, status14, status15
        };
    }

    /**
     * Returns the flipped stamp of every column in the grid in array order.
     */
    public long[] toStamps ()
    {
        return new long[] {
            flipped0, flipped1, flipped2, flipped3, flipped4, flipped5, flipped6, flipped7,
            flipped8, flipped9, flipped10, flipped11, flipped12, flipped13, flipped14, flipped15
        };
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SlotStatusRecord}
     * with the supplied key values.
     */
    public static Key<SlotStatusRecord> getKey (int userId)
    {
        return newKey(_R, userId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID); }
    // AUTO-GENERATED: METHODS END
}
