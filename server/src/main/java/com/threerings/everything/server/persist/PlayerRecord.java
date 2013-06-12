//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server.persist;

import java.sql.Timestamp;
import java.util.Set;
import java.util.TimeZone;

import com.google.common.base.Function;
import com.google.common.collect.Sets;

import com.samskivert.util.Calendars;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.util.RuntimeUtil;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.everything.data.FriendStatus;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Player;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.util.GameUtil;

/**
 * Maintains persistent state for a player.
 */
public class PlayerRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<PlayerRecord> _R = PlayerRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Long> FACEBOOK_ID = colexp(_R, "facebookId");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    public static final ColumnExp<String> SURNAME = colexp(_R, "surname");
    public static final ColumnExp<Integer> BIRTHDATE = colexp(_R, "birthdate");
    public static final ColumnExp<Integer> LAST_GIFT_YEAR = colexp(_R, "lastGiftYear");
    public static final ColumnExp<String> TIMEZONE = colexp(_R, "timezone");
    public static final ColumnExp<Boolean> IS_EDITOR = colexp(_R, "isEditor");
    public static final ColumnExp<Timestamp> JOINED = colexp(_R, "joined");
    public static final ColumnExp<Timestamp> LAST_SESSION = colexp(_R, "lastSession");
    public static final ColumnExp<Integer> COINS = colexp(_R, "coins");
    public static final ColumnExp<Integer> FREE_FLIPS = colexp(_R, "freeFlips");
    public static final ColumnExp<Integer> FLAGS = colexp(_R, "flags");
    public static final ColumnExp<Timestamp> NEXT_ATTRACTOR = colexp(_R, "nextAttractor");
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

    /** A function for converting this record to a {@link Player}. */
    public static Function<PlayerRecord, Player> TO_PLAYER =
        RuntimeUtil.makeToRuntime(PlayerRecord.class, Player.class);

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 13;

    /** The Samsara user id of this player. */
    @Id public int userId;

    /** This player's Facebook id. */
    public long facebookId;

    /** This player's first name (shown in some places in the game). */
    public String name;

    /** This player's last name (only shown to admins). */
    public String surname;

    /** The day of year on which this player's birthday falls as MMDD. */
    @Index public int birthdate;

    /** The year for which this player has last received a birthday gift. */
    @Column(defaultValue="2009")
    public int lastGiftYear;

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
    public int freeFlips;

    /** Flags tracked for this player. */
    public int flags;

    @Column(nullable=true)
    public Timestamp nextAttractor;

    /**
     * Sets the specified flag on this player.
     */
    public void setFlag (Player.Flag flag)
    {
        flags |= flag.getMask();
    }

    /**
     * Clears the specified flag from this player.
     */
    public void clearFlag (Player.Flag flag)
    {
        flags &= ~flag.getMask();
    }

    /**
     * Returns true if the specified flag is set for this player, false if not.
     */
    public boolean isSet (Player.Flag flag)
    {
        return (flags & flag.getMask()) != 0;
    }

    /**
     * Initializes {@link GameStatus#nextFlipCost}.
     */
    public int getNextFlipCost ()
    {
        return 0; // will be filled in by caller
    }

    /**
     * Initializes {@link Player#name} and {@link FriendStatus#name}.
     */
    public PlayerName getName ()
    {
        return TO_NAME.apply(this);
    }

    /**
     * Initializes {@link Player#flags}.
     */
    public Set<Player.Flag> getFlags ()
    {
        Set<Player.Flag> flags = Sets.newHashSet();
        for (Player.Flag flag : Player.Flag.values()) {
            if (isSet(flag)) {
                flags.add(flag);
            }
        }
        return flags;
    }

    /**
     * Reports a useful string identifying this player for logging.
     */
    public String who ()
    {
        return name + "/" + userId;
    }

    /**
     * Is this player relatively "new", having joined *approximately* within the specified
     * number of days?
     */
    public boolean isNewByDays (int days)
    {
        return (joined.getTime() + (GameUtil.ONE_DAY * days) > System.currentTimeMillis());
    }

    /**
     * Are we eligible to post an attractor card?
     */
    public boolean eligibleForAttractor ()
    {
        return (nextAttractor == null) || (nextAttractor.getTime() < System.currentTimeMillis());
    }

    /**
     * Calculate the next expire time for a generated grid, or gift record, for this player.
     */
    public Timestamp calculateNextExpires ()
    {
        TimeZone zone = TimeZone.getTimeZone(timezone);
        Calendars.Builder cal = Calendars.in(zone).zeroTime().addDays(1);
        long now = System.currentTimeMillis();
        while ((cal.toTime() - now) < MIN_EXPIRE_DURATION) {
            cal.addHours(1);
        }
        return cal.toTimestamp();
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link PlayerRecord}
     * with the supplied key values.
     */
    public static Key<PlayerRecord> getKey (int userId)
    {
        return newKey(_R, userId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID); }
    // AUTO-GENERATED: METHODS END

    /** The minimum allowed lifespan for a grid (and recruitment gifts) (2 hours). */
    protected static final long MIN_EXPIRE_DURATION = 2L * 60 * 60 * 1000L;
}
