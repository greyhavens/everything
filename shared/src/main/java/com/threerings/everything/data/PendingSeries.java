//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.data;

import java.io.Serializable;
import java.util.Set;

/**
 * Contains information on a pending series.
 */
public class PendingSeries
    implements Serializable
{
    /** The unique id for the series. */
    public int categoryId;

    /** The user id of the creator of this series. */
    public int creatorId;

    /** The name of the series. */
    public String name;

    /** The name of the series's subcategory. */
    public String subcategory;

    /** The name of the series's category. */
    public String category;

    /** The user ids of the "ship it" voters. */
    public Set<Integer> voters;
}
