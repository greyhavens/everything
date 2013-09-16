//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.rpc;

import com.threerings.everything.data.*;

/** Contains JSON model objects for the JSON RPC API. */
public interface JSON {

    // EveryService request classes
    class ValidateSession {
        public final String fbToken;
        public final int tzOffset;
        public ValidateSession (String fbToken, int tzOffset) {
            this.fbToken = fbToken;
            this.tzOffset = tzOffset;
        }
    }
    class GetUserFeed {
        public final int userId;
        public GetUserFeed (int userId) {
            this.userId = userId;
        }
    }
    class RedeemPurchase {
        public final String sku, platform, token, receipt;
        public RedeemPurchase (String sku, String platform, String token, String receipt) {
            this.sku = sku;
            this.platform = platform;
            this.token = token;
            this.receipt = receipt;
        }
    }

    // GameService request classes
    class GetCollection {
        public final int ownerId;
        public GetCollection (int ownerId) {
            this.ownerId = ownerId;
        }
    }
    class GetSeries {
        public final int ownerId;
        public final int categoryId;
        public GetSeries (int ownerId, int categoryId) {
            this.ownerId = ownerId;
            this.categoryId = categoryId;
        }
    }
    class GetGrid {
        public final Powerup pup;
        public final boolean expectHave;
        public GetGrid (Powerup pup, boolean expectHave) {
            this.pup = pup;
            this.expectHave = expectHave;
        }
    }
    class FlipCard {
        public final int gridId;
        public final int pos;
        public final int expectCost;
        public FlipCard (int gridId, int pos, int expectCost) {
            this.gridId = gridId;
            this.pos = pos;
            this.expectCost = expectCost;
        }
    }
    class CardInfo {
        public final int thingId;
        public final long created;
        public CardInfo (int thingId, long created) {
            this.thingId = thingId;
            this.created = created;
        }
    }
    class GiftCard {
        public final int thingId;
        public final long created;
        public final int friendId;
        public final String message;
        public GiftCard (int thingId, long created, int friendId, String message) {
            this.thingId = thingId;
            this.created = created;
            this.friendId = friendId;
            this.message = message;
        }
    }
    class SetLike {
        public final int catId;
        public final Boolean like;
        public SetLike (int catId, Boolean like) {
            this.catId = catId;
            this.like = like;
        }
    }
    class SetWant {
        public final int catId;
        public final boolean want;
        public SetWant (int catId, boolean want) {
            this.catId = catId;
            this.want = want;
        }
    }
    class BuyPowerup {
        public final Powerup pup;
        public BuyPowerup (Powerup pup) {
            this.pup = pup;
        }
    }
    class UsePowerup {
        public final int gridId;
        public final Powerup pup;
        public UsePowerup (int gridId, Powerup pup) {
            this.gridId = gridId;
            this.pup = pup;
        }
    }
}
