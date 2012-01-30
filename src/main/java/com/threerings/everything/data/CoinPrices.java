//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

/**
 * Contains our coin offers and their associated Facebook Credits cost.
 */
public class CoinPrices
{
    /** Tracks the enabledness of an offer. Switch to pending removal when disabling an offer; this
     * will allow it to be redeemed, but will not show it in the UI. Then an hour or two later,
     * update again with the offer as retired. */
    public static enum State { ACTIVE, PENDING_REMOVAL, RETIRED };

    /** Contains info on a single offer. */
    public static class Offer {
        /** A unique id for this offer. */
        public int id;

        /** Whether this offer is active. */
        public State state;

        /** The number of Everything coins received. */
        public int coins;

        /** The number of Facebook Credits paid. */
        public int credits;

        public Offer (int id, State state, int coins, int credits) {
            this.id = id;
            this.state = state;
            this.coins = coins;
            this.credits = credits;
        }
    }

    /** The deals on offer. */
    public static final Offer[] OFFERS = new Offer[] {
        // don't reuse unique ids
        new Offer(1, State.ACTIVE, 5000, 50),
        new Offer(2, State.ACTIVE, 11000, 100),
        new Offer(3, State.ACTIVE, 24000, 200),
    };

    /**
     * Returns the offer with the specified id.
     * @throws IllegalArgumentException if no (non-retired) offer exists with the supplied id.
     */
    public static Offer getOffer (int offerId)
    {
        for (Offer offer : OFFERS) {
            if (offer.id == offerId && offer.state != State.RETIRED) {
                return offer;
            }
        }
        throw new IllegalArgumentException("No offer with id " + offerId);
    }
}
