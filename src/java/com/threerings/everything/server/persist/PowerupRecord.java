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
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp TYPE = colexp(_R, "type");
    public static final ColumnExp COUNT = colexp(_R, "count");
    // AUTO-GENERATED: FIELDS END

    /** The player that owns this powerup. */
    @Id public int ownerId;

    /** The type of this powerup. */
    @Id public Powerup type;

    /** The number of charges remaining on this powerup (if applicable). */
    public int count;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link PowerupRecord}
     * with the supplied key values.
     */
    public static Key<PowerupRecord> getKey (int ownerId, int type)
    {
        return new Key<PowerupRecord>(
                PowerupRecord.class,
                new ColumnExp[] { OWNER_ID, TYPE },
                new Comparable[] { ownerId, type });
    }
    // AUTO-GENERATED: METHODS END
}
