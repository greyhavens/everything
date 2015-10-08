//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server.persist;

import com.google.common.base.Function;

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
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Integer> FRIEND_ID = colexp(_R, "friendId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

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
        return newKey(_R, userId, friendId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID, FRIEND_ID); }
    // AUTO-GENERATED: METHODS END

    /** A function for converting a FriendRecord to an Integer representing the friend's id. */
    public static Function<FriendRecord, Integer> TO_FRIEND_ID =
        new Function<FriendRecord, Integer>() {
            public Integer apply (FriendRecord record)
            {
                return record.friendId;
            }
        };
}
