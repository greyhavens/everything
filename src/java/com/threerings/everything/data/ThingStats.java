//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains stats on the Thing database.
 */
public class ThingStats
    implements IsSerializable
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
