//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a series.
 */
public class Series
    implements IsSerializable
{
    /** The maximum length of a series name. */
    public static final int MAX_NAME_LENGTH = 64;

    /** The unique identifier for this series. */
    public int seriesId;

    /** The name of this series. */
    public String name;

    /** The sub-category to which this series belongs. */
    public int categoryId;
}
