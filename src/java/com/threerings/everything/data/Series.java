//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains data on a single series (as owned by a player).
 */
public class Series
    implements IsSerializable
{
    /** The id of the category that identifies this series. */
    public int categoryId;

    /** The name of the series. */
    public String name;

    /** The things in the series, in rarity order. Things not owned by the player will be null. */
    public ThingCard[] things;
}
