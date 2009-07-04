//
// $Id$

package com.threerings.everything.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Contains data on a single thing.
 */
public class ThingRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ThingRecord> _R = ThingRecord.class;
    public static final ColumnExp THING_ID = colexp(_R, "thingId");
    public static final ColumnExp SET_ID = colexp(_R, "setId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp RARITY = colexp(_R, "rarity");
    public static final ColumnExp IMAGE = colexp(_R, "image");
    public static final ColumnExp DESCRIP = colexp(_R, "descrip");
    public static final ColumnExp FACTS = colexp(_R, "facts");
    // AUTO-GENERATED: FIELDS END

    /** A unique identifier for this thing. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int thingId;

    /** The set to which this thing belongs. */
    public int setId;

    /** The name of this thing. */
    public String name;

    /** This thing's rarity level (0-9). */
    public byte rarity;

    /** The name of this thing's image (its media hash). */
    public String image;

    /** This thing's description. */
    public String descrip;

    /** Facts about this thing. */
    @Column(length=2048)
    public String facts;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ThingRecord}
     * with the supplied key values.
     */
    public static Key<ThingRecord> getKey (int thingId)
    {
        return new Key<ThingRecord>(
                ThingRecord.class,
                new ColumnExp[] { THING_ID },
                new Comparable[] { thingId });
    }
    // AUTO-GENERATED: METHODS END
}
