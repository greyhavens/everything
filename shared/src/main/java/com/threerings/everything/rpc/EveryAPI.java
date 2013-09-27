//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.rpc;

import java.io.Serializable;
import java.util.List;

import com.threerings.everything.data.*;

/** Defines constants and classes used by the {@code everything} service. */
public interface EveryAPI {

    /** The path at which this API's servlets are mapped. */
    String ENTRY_POINT = "everything";

    /** Thrown by {@code validateSession} if we can't communicate with Facebook. */
    String E_FACEBOOK_DOWN = "e.facebook_down";

    /** Thrown by {@code getUserFeed} if the user in question does not exist. */
    String E_UNKNOWN_USER = "e.unknown_user";

    /** The billing platform code for the test device. */
    String PF_TEST = "TEST";

    /** The billing platform code for the Google Play Store. */
    String PF_PLAYSTORE = "PLAYSTORE";

    /** The billing platform code for the Apple App Store. */
    String PF_APPSTORE = "APPSTORE";

    /** Provides results for {@code getFeed}. */
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
