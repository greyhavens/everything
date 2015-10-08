//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.everything.data.Powerup;

public class PowerupPurchaseRecord extends PowerupRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<PowerupPurchaseRecord> _R = PowerupPurchaseRecord.class;
    public static final ColumnExp<Timestamp> AT = colexp(_R, "at");
    public static final ColumnExp<Integer> OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp<Powerup> TYPE = colexp(_R, "type");
    public static final ColumnExp<Integer> CHARGES = colexp(_R, "charges");
    // AUTO-GENERATED: FIELDS END

    /** The time at which the powerup was purchased. */
    @Id public Timestamp at;

    public static final int SCHEMA_VERSION = (PowerupRecord.SCHEMA_VERSION * 1000) + 1;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link PowerupPurchaseRecord}
     * with the supplied key values.
     */
    public static Key<PowerupPurchaseRecord> getKey (int ownerId, Powerup type, Timestamp at)
    {
        return newKey(_R, ownerId, type, at);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(OWNER_ID, TYPE, AT); }
    // AUTO-GENERATED: METHODS END
}
