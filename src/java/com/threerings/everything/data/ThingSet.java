//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a set.
 */
public class ThingSet
    implements IsSerializable
{
    /** The maximum length of a set name. */
    public static final int MAX_NAME_LENGTH = 64;

    /** The unique identifier for this set. */
    public int setId;

    /** The name of this set. */
    public String name;

    /** The sub-category to which this set belongs. */
    public int categoryId;
}
