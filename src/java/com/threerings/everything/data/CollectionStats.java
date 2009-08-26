//
// $Id$

package com.threerings.everything.data;

/**
 * Reports summary statistics on a player's collection.
 */
public class CollectionStats
{
    /** The owner of this collection. */
    public PlayerName owner;

    /** The total number of (unique) things in this user's collection. */
    public int things;

    /** The total number of series in which this user has at least one card. */
    public int series;

    /** The total number of complete series this user has. */
    public int completeSeries;

    /** The number of times this player has gifted cards. */
    public int gifts;
}
