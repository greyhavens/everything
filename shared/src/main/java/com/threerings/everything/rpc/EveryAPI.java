//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.rpc;

import java.io.Serializable;
import java.util.List;

import com.threerings.everything.data.*;

/** Defines classes used by the {@code everything} service. */
public interface EveryAPI {

    /** Provides results for {@code getRecentFeed}. */
    class FeedResult implements Serializable {
        /** The "recruitment gifts" for you to use to slurp in new players, or null to mark
         * a slot as already gifted. */
        public List<Card> recruitGifts;
        /** Gifts awaiting this player, if any. */
        public List<ThingCard> gifts;
        /** Comments on this user's series. */
        public List<CategoryComment> comments;
        /** This user's recent feed. */
        public List<FeedItem> items;
    }

    /** Provides results for {@code getCredits}. */
    class CreditsResult implements Serializable {
        public PlayerName design;
        public PlayerName art;
        public List<PlayerName> code;
        public List<PlayerName> editors;
    }
}
