//
// $Id$

package com.threerings.everything.data;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains summary information for a player's entire collection.
 */
public class PlayerCollection
    implements IsSerializable
{
    /** The owner of this collection. */
    public PlayerName owner;

    /** A mapping from category -> sub-category -> series card(s) for the entire collection. */
    public Map<String, Map<String, List<SeriesCard>>> series;
}
