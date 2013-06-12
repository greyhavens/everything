//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.io.Serializable;

/**
 * Contains stats on the Thing database.
 */
public class ThingStats
    implements Serializable
{
    /** The total number of things in the database. */
    public int totalThings;

    /** The total number of categories in the database. */
    public int totalCategories;

    /** The total number of players in the database. */
    public int totalPlayers;

    /** The total number of cards in the database. */
    public int totalCards;
}
