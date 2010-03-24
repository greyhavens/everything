//
// $Id$

package com.threerings.everything.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Tracks whether a user likes or dislikes a particular series.
 * If a player is "neutral" on a particular series, then this record will be omitted for it.
 *
 * The categoryId is used to specify a seriesId; we do not presently let players like or dislike
 * higher-level categories.
 */
public class LikeRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<LikeRecord> _R = LikeRecord.class;
    public static final ColumnExp USER_ID = colexp(_R, "userId");
    public static final ColumnExp CATEGORY_ID = colexp(_R, "categoryId");
    public static final ColumnExp LIKE = colexp(_R, "like");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The user has is expressing a preference. */
    @Id public int userId;

    /** The category to which this preference applies. */
    @Id public int categoryId;

    /** Do they like the category, or dislike it? */
    public boolean like;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link LikeRecord}
     * with the supplied key values.
     */
    public static Key<LikeRecord> getKey (int userId, int categoryId)
    {
        return newKey(_R, userId, categoryId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID, CATEGORY_ID); }
    // AUTO-GENERATED: METHODS END
}
