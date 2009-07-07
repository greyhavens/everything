//
// $Id$

package com.threerings.everything.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Represents a category that a player wishes to complete.
 */
public class WishRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<WishRecord> _R = WishRecord.class;
    public static final ColumnExp USER_ID = colexp(_R, "userId");
    public static final ColumnExp CATEGORY_ID = colexp(_R, "categoryId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The user that has this wish. */
    @Id public int userId;

    /** The category this user wishes to complete.*/
    @Id public int categoryId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link WishRecord}
     * with the supplied key values.
     */
    public static Key<WishRecord> getKey (int userId, int categoryId)
    {
        return new Key<WishRecord>(
                WishRecord.class,
                new ColumnExp[] { USER_ID, CATEGORY_ID },
                new Comparable[] { userId, categoryId });
    }
    // AUTO-GENERATED: METHODS END
}
