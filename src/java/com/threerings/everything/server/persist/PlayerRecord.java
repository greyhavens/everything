//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Date;
import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.everything.data.PlayerName;

/**
 * Maintains persistent state for a player.
 */
public class PlayerRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<PlayerRecord> _R = PlayerRecord.class;
    public static final ColumnExp USER_ID = colexp(_R, "userId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp COINS = colexp(_R, "coins");
    public static final ColumnExp BIRTHDAY = colexp(_R, "birthday");
    public static final ColumnExp JOINED = colexp(_R, "joined");
    public static final ColumnExp LAST_SESSION = colexp(_R, "lastSession");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 3;

    /** The Samsara user id of this player. */
    @Id public int userId;

    /** This player's name. */
    public String name;

    /** This player's current coin balance. */
    public int coins;

    /** This player's birthday (we'll send them something nice on their birthday). */
    @Column(nullable=true)
    public Date birthday;

    /** The time this player started playing the game. */
    public Timestamp joined;

    /** The time this player last started a game session. */
    @Index public Timestamp lastSession;

    /**
     * Creates a {@link PlayerName} instance from our data.
     */
    public PlayerName toName ()
    {
        PlayerName pname = new PlayerName();
        pname.userId = userId;
        pname.name = name;
        return pname;
    }

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
