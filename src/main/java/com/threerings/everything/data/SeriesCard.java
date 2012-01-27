//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a player's series.
 */
public class SeriesCard
    implements IsSerializable, Comparable<SeriesCard>
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

    /**
     * Returns a glyph representing the completeness of this series.
     */
    public String glyph ()
    {
        float pct = owned / (float)things;
        if (pct < 0.25) {
            return "\u25CB";
        } else if (pct < 0.5) {
            return "\u25D4";
        } else if (pct < 0.75) {
            return "\u25D1";
        } else if (pct < 1) {
            return "\u25D5";
        } else {
            return "\u25CF";
        }
    }

    // from interface Comparable<SeriesCard>
    public int compareTo (SeriesCard other)
    {
        return name.compareTo(other.name);
    }
}
