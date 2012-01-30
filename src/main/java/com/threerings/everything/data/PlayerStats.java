//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Reports summary statistics on a player.
 */
public class PlayerStats
    implements IsSerializable
{
    /** The player in question. */
    public PlayerName name;

    /** The total number of (unique) things in this user's collection. */
    public int things;

    /** The total number of series in which this user has at least one card. */
    public int series;

    /** The total number of complete series this user has. */
    public int completeSeries;

    /** The number of times this player has gifted cards. */
    public int gifts;

    /** The time at which this player was last online. */
    public Date lastSession;
}
