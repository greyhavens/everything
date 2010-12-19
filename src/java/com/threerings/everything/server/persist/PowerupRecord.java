//
// $Id$

package com.threerings.everything.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.everything.data.Powerup;

/**
 * Represents a powerup in the possession of a player.
 */
public class PowerupRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<PowerupRecord> _R = PowerupRecord.class;
    public static final ColumnExp<Integer> OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp<Powerup> TYPE = colexp(_R, "type");
    public static final ColumnExp<Integer> CHARGES = colexp(_R, "charges");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** The player that owns this powerup. */
    @Id public int ownerId;

    /** The type of this powerup. */
    @Id public Powerup type;

    /** The number of charges remaining on this powerup. */
    public int charges;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link PowerupRecord}
     * with the supplied key values.
     */
    public static Key<PowerupRecord> getKey (int ownerId, Powerup type)
    {
        return newKey(_R, ownerId, type);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(OWNER_ID, TYPE); }
    // AUTO-GENERATED: METHODS END
}
