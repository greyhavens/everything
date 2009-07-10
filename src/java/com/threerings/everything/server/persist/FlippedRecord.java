//
// $Id$

package com.threerings.everything.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Maintains the flipped status for each slot in a player's active grid. We track the status of
 * each slot in a separate column so that we can use the database to prevent funny business if the
 * user tries to flip cards from multiple clients at the same instant.
 */
public class FlippedRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FlippedRecord> _R = FlippedRecord.class;
    public static final ColumnExp USER_ID = colexp(_R, "userId");
    public static final ColumnExp SLOT0 = colexp(_R, "slot0");
    public static final ColumnExp SLOT1 = colexp(_R, "slot1");
    public static final ColumnExp SLOT2 = colexp(_R, "slot2");
    public static final ColumnExp SLOT3 = colexp(_R, "slot3");
    public static final ColumnExp SLOT4 = colexp(_R, "slot4");
    public static final ColumnExp SLOT5 = colexp(_R, "slot5");
    public static final ColumnExp SLOT6 = colexp(_R, "slot6");
    public static final ColumnExp SLOT7 = colexp(_R, "slot7");
    public static final ColumnExp SLOT8 = colexp(_R, "slot8");
    public static final ColumnExp SLOT9 = colexp(_R, "slot9");
    public static final ColumnExp SLOT10 = colexp(_R, "slot10");
    public static final ColumnExp SLOT11 = colexp(_R, "slot11");
    public static final ColumnExp SLOT12 = colexp(_R, "slot12");
    public static final ColumnExp SLOT13 = colexp(_R, "slot13");
    public static final ColumnExp SLOT14 = colexp(_R, "slot14");
    public static final ColumnExp SLOT15 = colexp(_R, "slot15");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** Our slot columns, in grid position order. */
    public static ColumnExp[] SLOTS = {
        SLOT0, SLOT1, SLOT2, SLOT3, SLOT4, SLOT5, SLOT6, SLOT7,
        SLOT8, SLOT9, SLOT10, SLOT11, SLOT12, SLOT13, SLOT14, SLOT15
    };

    /** The user for whom we're tracking flipped status. */
    @Id public int userId;

    /** Whether or not slot 0 has been flipped. */
    public boolean slot0;

    /** Whether or not slot 1 has been flipped. */
    public boolean slot1;

    /** Whether or not slot 2 has been flipped. */
    public boolean slot2;

    /** Whether or not slot 3 has been flipped. */
    public boolean slot3;

    /** Whether or not slot 4 has been flipped. */
    public boolean slot4;

    /** Whether or not slot 5 has been flipped. */
    public boolean slot5;

    /** Whether or not slot 6 has been flipped. */
    public boolean slot6;

    /** Whether or not slot 7 has been flipped. */
    public boolean slot7;

    /** Whether or not slot 8 has been flipped. */
    public boolean slot8;

    /** Whether or not slot 9 has been flipped. */
    public boolean slot9;

    /** Whether or not slot 10 has been flipped. */
    public boolean slot10;

    /** Whether or not slot 11 has been flipped. */
    public boolean slot11;

    /** Whether or not slot 12 has been flipped. */
    public boolean slot12;

    /** Whether or not slot 13 has been flipped. */
    public boolean slot13;

    /** Whether or not slot 14 has been flipped. */
    public boolean slot14;

    /** Whether or not slot 15 has been flipped. */
    public boolean slot15;

    /**
     * Converts the contents of this record into a boolean array.
     */
    public boolean[] toFlipped ()
    {
        return new boolean[] {
            slot0, slot1, slot2, slot3, slot4, slot5, slot6, slot7,
            slot8, slot9, slot10, slot11, slot12, slot13, slot14, slot15
        };
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FlippedRecord}
     * with the supplied key values.
     */
    public static Key<FlippedRecord> getKey (int userId)
    {
        return new Key<FlippedRecord>(
                FlippedRecord.class,
                new ColumnExp[] { USER_ID },
                new Comparable[] { userId });
    }
    // AUTO-GENERATED: METHODS END
}
