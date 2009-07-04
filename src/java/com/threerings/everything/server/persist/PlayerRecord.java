//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Maintains persistent state for a player.
 */
public class PlayerRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<PlayerRecord> _R = PlayerRecord.class;
    public static final ColumnExp USER_ID = colexp(_R, "userId");
    public static final ColumnExp COINS = colexp(_R, "coins");
    public static final ColumnExp JOINED = colexp(_R, "joined");
    public static final ColumnExp LAST_SESSION = colexp(_R, "lastSession");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The Samsara user id of this player. */
    @Id public int userId;

    /** This player's name. */
    public String name;

    /** This player's current coin balance. */
    public int coins;

    /** The time this player started playing the game. */
    public Timestamp joined;

    /** The time this player last started a game session. */
    public Timestamp lastSession;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link PlayerRecord}
     * with the supplied key values.
     */
    public static Key<PlayerRecord> getKey (int userId)
    {
        return new Key<PlayerRecord>(
                PlayerRecord.class,
                new ColumnExp[] { USER_ID },
                new Comparable[] { userId });
    }
    // AUTO-GENERATED: METHODS END
}
