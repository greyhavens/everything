//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.data;

import java.io.Serializable;
import java.util.Date;

/**
 * Reports summary statistics on a player.
 */
public class PlayerStats
    implements Serializable
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
