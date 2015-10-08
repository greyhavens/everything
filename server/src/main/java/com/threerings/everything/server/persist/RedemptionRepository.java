//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server.persist;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;

@Singleton
public class RedemptionRepository extends DepotRepository
{
    @Inject public RedemptionRepository (PersistenceContext ctx) {
        super(ctx);
    }

    /**
     * Notes that particular purchase has been redeemed for coins (via the unique purchase
     * identifier supplied by the payment provider, e.g. Google, App Store, etc.). This is only
     * used for mobile payments where the payment happens on the device and the server only finds
     * out about it after the money has already been spent. For server-based payments (i.e.
     * Facebook), we process the payment and deliver the coins in one fell swoop on the server and
     * don't need to track the redemption of receipts.
     *
     * @param token a unique string identifying the purchase (generated by the payment provider).
     * @param userId the id of the user that is redeeming the purchase.
     * @param platform the identifier of the platform via which the purchase was made.
     *
     * @return true if the purchase token has not already been redeemed and is now marked as
     * redeemed, false if the specified purchase token has already been redeemed.
     */
    public boolean noteRedemption (String token, int userId, String platform) {
        try {
            RedemptionRecord record = new RedemptionRecord();
            record.token = token;
            record.userId = userId;
            record.platform = platform;
            insert(record);
            return true;
        } catch (DuplicateKeyException dke) {
            return false;
        }
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes) {
        classes.add(RedemptionRecord.class);
    }
}
