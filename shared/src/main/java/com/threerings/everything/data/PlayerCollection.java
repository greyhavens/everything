//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contains summary information for a player's entire collection.
 */
public class PlayerCollection
    implements Serializable
{
    /** The owner of this collection. */
    public PlayerName owner;

    /** A mapping from category -> sub-category -> series card(s) for the entire collection. */
    public Map<String, Map<String, List<SeriesCard>>> series;

    /** The trophies this player has earned, ordered from most-recently earned, or null. */
    public List<TrophyData> trophies;

    /** The ids of the categories liked by this player. */
    public Set<Integer> likes;

    /**
     * Returns the number of (unique) cards in this collection.
     */
    public int countCards ()
    {
        int count = 0;
        for (Map<String, List<SeriesCard>> cat : series.values()) {
            for (List<SeriesCard> subcat : cat.values()) {
                for (SeriesCard series : subcat) {
                    count += series.owned;
                }
            }
        }
        return count;
    }

    /**
     * Returns the number of series in this collection.
     */
    public int countSeries ()
    {
        int count = 0;
        for (Map<String, List<SeriesCard>> cat : series.values()) {
            for (List<SeriesCard> subcat : cat.values()) {
                count += subcat.size();
            }
        }
        return count;
    }

    /**
     * Returns the number of completed series in this collection.
     */
    public int countCompletedSeries ()
    {
        int count = 0;
        for (Map<String, List<SeriesCard>> cat : series.values()) {
            for (List<SeriesCard> subcat : cat.values()) {
                for (SeriesCard series : subcat) {
                    if (series.owned == series.things) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
