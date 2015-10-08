//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.depot.PersistentRecord;

/**
 * Notes the redemption of a particular purchase.
 */
public class RedemptionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<RedemptionRecord> _R = RedemptionRecord.class;
    public static final ColumnExp<String> TOKEN = colexp(_R, "token");
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<String> PLATFORM = colexp(_R, "platform");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The token that identifies the purchase. */
    @Id public String token;

    /** The user that redeemed the purchase. */
    public int userId;

    /** The platform via which this purchase was made. */
    public String platform;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link RedemptionRecord}
     * with the supplied key values.
     */
    public static Key<RedemptionRecord> getKey (String token)
    {
        return newKey(_R, token);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(TOKEN); }
    // AUTO-GENERATED: METHODS END
}
