//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Date;
import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.util.RuntimeUtil;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.everything.client.GameCodes;
import com.threerings.everything.data.FriendStatus;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.PlayerDetails;
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
    public static final ColumnExp SURNAME = colexp(_R, "surname");
    public static final ColumnExp BIRTHDAY = colexp(_R, "birthday");
    public static final ColumnExp TIMEZONE = colexp(_R, "timezone");
    public static final ColumnExp IS_EDITOR = colexp(_R, "isEditor");
    public static final ColumnExp JOINED = colexp(_R, "joined");
    public static final ColumnExp LAST_SESSION = colexp(_R, "lastSession");
    public static final ColumnExp COINS = colexp(_R, "coins");
    public static final ColumnExp FREE_FLIPS = colexp(_R, "freeFlips");
    // AUTO-GENERATED: FIELDS END

    /** A function for converting this record to a {@link PlayerName}. */
    public static Function<PlayerRecord, PlayerName> TO_NAME =
        RuntimeUtil.makeToRuntime(PlayerRecord.class, PlayerName.class);

    /** A function for converting this record to a {@link GameStatus}. */
    public static Function<PlayerRecord, GameStatus> TO_GAME_STATUS =
        RuntimeUtil.makeToRuntime(PlayerRecord.class, GameStatus.class);

    /** A function for converting this record to a {@link FriendStatus}. */
    public static Function<PlayerRecord, FriendStatus> TO_FRIEND_STATUS =
        RuntimeUtil.makeToRuntime(PlayerRecord.class, FriendStatus.class);

    /** A function for converting this record to a {@link GameStatus}. */
    public static Function<PlayerRecord, PlayerDetails> TO_DETAILS =
        RuntimeUtil.makeToRuntime(PlayerRecord.class, PlayerDetails.class);

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 6;

    /** The Samsara user id of this player. */
    @Id public int userId;

    /** This player's first name (shown in some places in the game). */
    public String name;

    /** This player's last name (only shown to admins). */
    @Column(defaultValue="''") // temp
    public String surname;

    /** This player's birthday (we'll send them something nice on their birthday). */
    @Column(nullable=true)
    public Date birthday;

    /** This player's preferred timezone (which dictates when their grids expire). */
    public String timezone;

    /** Whether or not this player has editor privileges. */
    public boolean isEditor;

    /** The time this player started playing the game. */
    public Timestamp joined;

    /** The time this player last started a game session. */
    @Index public Timestamp lastSession;

    /** This player's current coin balance. */
    public int coins;

    /** This player's accumulated free flips. */
    public float freeFlips;

    /**
     * Initializes {@link GameStatus#freeFlips}.
     */
    public int getFreeFlips ()
    {
        return (int)Math.floor(freeFlips);
    }

    /**
     * Initializes {@link GameStatus#nextFlipCost}.
     */
    public int getNextFlipCost ()
    {
        return 0; // will be filled in by caller
    }

    /**
     * Initializes {@link GameStatus#nextFreeFlipAt}.
     */
    public long getNextFreeFlipAt ()
    {
        long flipsPerDay = (freeFlips < GameCodes.DAILY_FREE_FLIPS) ?
            GameCodes.DAILY_FREE_FLIPS : GameCodes.VACATION_FREE_FLIPS;
        long millisPerFlip = ONE_DAY / flipsPerDay;
        long millisToNextFlip = (long)(millisPerFlip * (Math.ceil(freeFlips) - freeFlips));
        return lastSession.getTime() + millisToNextFlip;
    }

    /**
     * Initializes {@link PlayerDetails#name} and  {@link FriendStatus#name}.
     */
    public PlayerName getName ()
    {
        return TO_NAME.apply(this);
    }

    /**
     * Reports a useful string identifying this player for logging.
     */
    public String who ()
    {
        return name + "/" + userId;
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

    /** One day in milliseconds. */
    protected static final long ONE_DAY = 24 * 60 * 60 * 1000L;
}
