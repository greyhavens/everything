//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.rpc;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.threerings.everything.data.*;

/** Defines classes used by the {@code game} service. */
public interface GameAPI {

    /** The path at which this API's servlets are mapped. */
    String ENTRY_POINT = "game";

    /** Thrown by {@code getCollection} if the user in question does not exist. */
    String E_UNKNOWN_USER = "e.unknown_user";

    /** Thrown by {@code getSeries} if the series in question does not exist. */
    String E_UNKNOWN_SERIES = "e.unknown_series";

    /** Thrown by {@code getGrid} if the player has too few series for the ALL_COLLECTED_SERIES
     * powerup. */
    String E_TOO_FEW_SERIES = "e.too_few_series";

    /** Thrown by {@code flipCard} or {@code usePowerup} if the grid in question has expired. */
    String E_GRID_EXPIRED = "e.grid_expired";

    /** Thrown by {@code flipCard} if the position requested has already been flipped. */
    String E_ALREADY_FLIPPED = "e.already_flipped";

    /** Thrown by {@code flipCard} if the user thinks they get a free flip but don't have one. */
    String E_LACK_FREE_FLIP = "e.lack_free_flip";

    /** Thrown by {@code flipCard} if the user's expected flip cost doesn't match the server's. */
    String E_FLIP_COST_CHANGED = "e.flip_cost_changed";

    /** Thrown by {@code flipCard} if the user can't afford the flip they requested. */
    String E_NSF_FOR_FLIP = "e.nsf_for_flip";

    /** Thrown by various card-related methods if the card in question does not exist. */
    String E_UNKNOWN_CARD = "e.unknown_card";

    /** Thrown by {@code buyPowerup} if the powerup is permanent and the player already owns it. */
    String E_ALREADY_OWN_POWERUP = "e.already_own_powerup";

    /** Thrown by {@code buyPowerup} if the user can't afford the powerup they requested. */
    String E_NSF_FOR_PURCHASE = "e.nsf_for_purchase";

    /** Thrown by {@code usePower} if the user doesn't have a charge of the specified powerup. */
    String E_LACK_CHARGE = "e.lack_charge";

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
