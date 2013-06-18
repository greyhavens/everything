//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.rpc;

import java.util.Map;

import com.threerings.everything.data.*;

/** Contains JSON model objects for the JSON RPC API. */
public class JSON {

    // EveryService request classes
    public static class ValidateSession {
        public String fbId;
        public String fbToken;
        public int tzOffset;
        public ValidateSession (String fbId, String fbToken, int tzOffset) {
            this.fbId = fbId;
            this.fbToken = fbToken;
            this.tzOffset = tzOffset;
        }
    }
    public static class GetUserFeed {
        public int userId;
        public GetUserFeed (int userId) {
            this.userId = userId;
        }
    }

    // GameService request classes
    public static class GetCollection {
        public int ownerId;
        public GetCollection (int ownerId) {
            this.ownerId = ownerId;
        }
    }
    public static class GetSeries {
        public int ownerId;
        public int categoryId;
        public GetSeries (int ownerId, int categoryId) {
            this.ownerId = ownerId;
            this.categoryId = categoryId;
        }
    }
    public static class GetGrid {
        public Powerup pup;
        public boolean expectHave;
        public GetGrid (Powerup pup, boolean expectHave) {
            this.pup = pup;
            this.expectHave = expectHave;
        }
    }
    public static class FlipCard {
        public int gridId;
        public int pos;
        public int expectCost;
        public FlipCard (int gridId, int pos, int expectCost) {
            this.gridId = gridId;
            this.pos = pos;
            this.expectCost = expectCost;
        }
    }
    public static class CardInfo {
        public int thingId;
        public long created;
        public CardInfo (int thingId, long created) {
            this.thingId = thingId;
            this.created = created;
        }
    }
    public static class GiftCard {
        public int thingId;
        public long created;
        public int friendId;
        public String message;
        public GiftCard (int thingId, long created, int friendId, String message) {
            this.thingId = thingId;
            this.created = created;
            this.friendId = friendId;
            this.message = message;
        }
    }
    public static class SetLike {
        public int catId;
        public Boolean like;
        public SetLike (int catId, Boolean like) {
            this.catId = catId;
            this.like = like;
        }
    }
    public static class BuyPowerup {
        public Powerup pup;
        public BuyPowerup (Powerup pup) {
            this.pup = pup;
        }
    }
    public static class UsePowerup {
        public int gridId;
        public Powerup pup;
        public UsePowerup (int gridId, Powerup pup) {
            this.gridId = gridId;
            this.pup = pup;
        }
    }

    // GameService response classes
    public static class GridResult {
        public Grid grid;
        public GameStatus status;
        public GridResult (Grid grid, GameStatus status) {
            this.grid = grid;
            this.status = status;
        }
    }
    public static class CardResult {
        public Card card;
        public int haveCount;
        public int thingsRemaining;
        public TrophyData[] trophies;
        public CardResult (Card card, int haveCount, int thingsRemaining, TrophyData[] trophies) {
            this.card = card;
            this.haveCount = haveCount;
            this.thingsRemaining = thingsRemaining;
            this.trophies = trophies;
        }
    }
    public static class FlipCardResult extends CardResult {
        public GameStatus status;
        public FlipCardResult (Card card, int haveCount, int thingsRemaining, TrophyData[] trophies,
                               GameStatus status) {
            super(card, haveCount, thingsRemaining, trophies);
            this.status = status;
        }
    }
    public static class SellCardResult {
        public int coins;
        public Boolean newLike;
        public SellCardResult (int coins, Boolean newLike) {
            this.coins = coins;
            this.newLike = newLike;
        }
    }
    public static class GiftCardInfo {
        public int thingCount;
        public FriendCardInfo[] friends;
        public GiftCardInfo (int thingCount, FriendCardInfo[] friends) {
            this.thingCount = thingCount;
            this.friends = friends;
        }
    }
    public static class GiftInfo extends CardResult {
        public String message;
        public GiftInfo (Card card, int haveCount, int thingsRemaining, TrophyData[] trophies,
                         String message) {
            super(card, haveCount, thingsRemaining, trophies);
            this.message = message;
        }
    }
    public static class ShopInfo {
        public int coins;
        public Map<Powerup,Integer> pups;
        public ShopInfo (int coins, Map<Powerup,Integer> pups) {
            this.coins = coins;
            this.pups = pups;
        }
    }
}
