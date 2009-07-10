//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a player's series.
 */
public class SeriesCard
    implements IsSerializable
{
    /** The category that identifies this series. */
    public int categoryId;

    /** The name of this series. */
    public String name;

    /** The parent of this series' category. */
    public int parentId;

    /** The number of things in the series .*/
    public int things;

    /** The number of cards in the series that the player owns .*/
    public int owned;
}
