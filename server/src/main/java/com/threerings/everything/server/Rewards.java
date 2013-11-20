//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.server;

import com.threerings.everything.data.Notice;

/** A place to keep track of reward amounts. */
public class Rewards
{
    /** The number of free coins given to a new player. */
    public static final int NEW_USER_FREE_COINS = 2000;

    /** The number of coins to grant to a web player that tries the mobile client. */
    public static final int PLAYED_MOBILE_COINS = 2000;

    /** The number of coins to grant to a player whose friend starts playing. */
    public static final int FRIEND_JOINED_COINS = 500;

    public static Notice friendJoined (String name) {
        return new Notice(Notice.Kind.FRIEND_JOINED, name, FRIEND_JOINED_COINS);
    }

    public static Notice playedMobile () {
        return new Notice(Notice.Kind.PLAYED_MOBILE, null, PLAYED_MOBILE_COINS);
    }
}
