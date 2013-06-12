//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.io.Serializable;

/**
 * Contains data on a single series (as owned by a player).
 */
public class Series
    implements Serializable
{
    /** The id of the category that identifies this series. */
    public int categoryId;

    /** The name of the series. */
    public String name;

    /** The creator of this series. */
    public PlayerName creator;

    /** The things in the series, in rarity order. Things not owned by the player will be null. */
    public ThingCard[] things;
}
