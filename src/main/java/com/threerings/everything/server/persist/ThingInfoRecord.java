//
// $Id$

package com.threerings.everything.server.persist;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Computed;

import com.threerings.everything.data.Rarity;

/**
 * Used to extract just the information needed to build our {@link ThingIndex}.
 */
@Computed(shadowOf=ThingRecord.class)
public class ThingInfoRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ThingInfoRecord> _R = ThingInfoRecord.class;
    public static final ColumnExp<Integer> THING_ID = colexp(_R, "thingId");
    public static final ColumnExp<Integer> CATEGORY_ID = colexp(_R, "categoryId");
    public static final ColumnExp<Rarity> RARITY = colexp(_R, "rarity");
    // AUTO-GENERATED: FIELDS END

    /** A unique identifier for this thing. */
    public int thingId;

    /** The (leaf) category to which this thing belongs. */
    public int categoryId;

    /** This thing's rarity level. */
    public Rarity rarity;
}
