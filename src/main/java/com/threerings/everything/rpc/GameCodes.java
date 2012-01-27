//
// $Id$

package com.threerings.everything.rpc;

/**
 * Contains codes and constants pertaining to the Everything gameplay.
 */
public final class GameCodes
{
    /** The number of free coins given to a new player. */
    public static final int NEW_USER_FREE_COINS = 100;

    /** The number of free flips earned during the first day that a player is away. */
    public static final int DAILY_FREE_FLIPS = 5;

    /** The number of free flips earned per day after the first day. */
    public static final int VACATION_FREE_FLIPS = 1;

    /** The maximum number of free flips you can get, regardless of how long you've been away. */
    public static final int MAX_FREE_FLIPS = 32;

    /** Payment to a creator (per card) when their series is accepted into the database. */
    public static final int COINS_PER_CREATED_CARD = 25;
}
