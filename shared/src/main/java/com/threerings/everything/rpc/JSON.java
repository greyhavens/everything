//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.rpc;

import com.threerings.everything.data.*;

/** Contains JSON model objects for the JSON RPC API. */
public interface JSON {

    // EveryService request classes
    class ValidateSession {
        public String fbToken;
        public int tzOffset;
        public ValidateSession (String fbToken, int tzOffset) {
            this.fbToken = fbToken;
            this.tzOffset = tzOffset;
        }
    }
    class GetUserFeed {
        public int userId;
        public GetUserFeed (int userId) {
            this.userId = userId;
        }
    }

    // GameService request classes
    class GetCollection {
        public int ownerId;
        public GetCollection (int ownerId) {
            this.ownerId = ownerId;
        }
    }
    class GetSeries {
        public int ownerId;
        public int categoryId;
        public GetSeries (int ownerId, int categoryId) {
            this.ownerId = ownerId;
            this.categoryId = categoryId;
        }
    }
    class GetGrid {
        public Powerup pup;
        public boolean expectHave;
        public GetGrid (Powerup pup, boolean expectHave) {
            this.pup = pup;
            this.expectHave = expectHave;
        }
    }
    class FlipCard {
        public int gridId;
        public int pos;
        public int expectCost;
        public FlipCard (int gridId, int pos, int expectCost) {
            this.gridId = gridId;
            this.pos = pos;
            this.expectCost = expectCost;
        }
    }
    class CardInfo {
        public int thingId;
        public long created;
        public CardInfo (int thingId, long created) {
            this.thingId = thingId;
            this.created = created;
        }
    }
    class GiftCard {
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
    class SetLike {
        public int catId;
        public Boolean like;
        public SetLike (int catId, Boolean like) {
            this.catId = catId;
            this.like = like;
        }
    }
    class BuyPowerup {
        public Powerup pup;
        public BuyPowerup (Powerup pup) {
            this.pup = pup;
        }
    }
    class UsePowerup {
        public int gridId;
        public Powerup pup;
        public UsePowerup (int gridId, Powerup pup) {
            this.gridId = gridId;
            this.pup = pup;
        }
    }
}
