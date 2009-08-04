//
// $Id$

package com.threerings.everything.server.persist;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Computed;

/**
 * Used to count up how many of each card a player owns in a particular series.
 */
@Computed(shadowOf=CardRecord.class)
public class OwnerRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<OwnerRecord> _R = OwnerRecord.class;
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp COUNT = colexp(_R, "count");
    // AUTO-GENERATED: FIELDS END

    /** The id of the player in question. */
    public int ownerId;

    /** The number of cards owned in this series. */
    @Computed(fieldDefinition="count(distinct \"thingId\")")
    public int count;
}
