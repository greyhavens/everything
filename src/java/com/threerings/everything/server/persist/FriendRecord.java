//
// $Id$

package com.threerings.everything.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Tracks a friend relationship between two players.
 */
public class FriendRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FriendRecord> _R = FriendRecord.class;
    public static final ColumnExp USER_ID = colexp(_R, "userId");
    public static final ColumnExp FRIEND_ID = colexp(_R, "friendId");
    // AUTO-GENERATED: FIELDS END

    /** The id of the user in question. */
    @Id public int userId;

    /** The id of this user's friend. */
    @Id public int friendId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FriendRecord}
     * with the supplied key values.
     */
    public static Key<FriendRecord> getKey (int userId, int friendId)
    {
        return new Key<FriendRecord>(
                FriendRecord.class,
                new ColumnExp[] { USER_ID, FRIEND_ID },
                new Comparable[] { userId, friendId });
    }
    // AUTO-GENERATED: METHODS END
}
