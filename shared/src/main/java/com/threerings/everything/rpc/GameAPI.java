//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.rpc;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.threerings.everything.data.*;

/** Defines classes used by the {@code game} service. */
public interface GameAPI {

    /** Provides results for {@code getGrid}. */
    class GridResult implements Serializable {
        /** The player's current grid. */
        public Grid grid;
        /** The player's current game status. */
        public GameStatus status;
    }

    /** A base class shared by {@link FlipResult} and {@link GiftResult}. */
    class CardResult implements Serializable {
        /** The card in question. */
        public Card card;
        /** Number of this thing already held by this player (not including this one). */
        public int haveCount;
        /** Number of things remaining in this set not held by this player. */
        public int thingsRemaining;
        /** Trophies newly earned, or null. */
        public List<TrophyData> trophies;
    }

    /** Provides results for {@code flipCard}. */
    class FlipResult extends CardResult {
        /** The player's new game status after the flip. */
        public GameStatus status;
        /** Do we want to duplicate this card as a bonanza? */
        public boolean bonanza;
    }

    /** Provides results for {@code getGiftCardInfo}. */
    class GiftInfoResult implements Serializable {
        /** The number of things in the series of the card being considered for gifting. */
        public int things;
        /** The status of each of this player's friends that do not already have the card. */
        public List<FriendCardInfo> friends;
    }

    /** Provides results for {@code openGift}. */
    class GiftResult extends CardResult {
        /** The message from the gifter, if any. */
        public String message;
    }

    /** Provides results for {@code getShopInfo}. */
    class ShopResult implements Serializable {
        /** This player's current coin balance. */
        public int coins;
        /** This player's current powerups count. */
        public Map<Powerup, Integer> powerups;
    }

    /** Provides results for {@code sellCard}. */
    class SellResult implements Serializable {
        /** The player's new coin balance. */
        public int coins;
        /** The new 'like' value for this category. */
        public Boolean newLike;
    }
}
