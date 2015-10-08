//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.util.RuntimeUtil;

import com.threerings.everything.data.Grid;
import com.threerings.everything.data.GridStatus;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.SlotStatus;
import com.threerings.everything.data.ThingCard;

/**
 * Contains data on a player's current grid.
 */
public class GridRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GridRecord> _R = GridRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Integer> GRID_ID = colexp(_R, "gridId");
    public static final ColumnExp<GridStatus> STATUS = colexp(_R, "status");
    public static final ColumnExp<int[]> THING_IDS = colexp(_R, "thingIds");
    public static final ColumnExp<Timestamp> EXPIRES = colexp(_R, "expires");
    // AUTO-GENERATED: FIELDS END

    /** A function for converting persistent records to runtime records. */
    public static Function<GridRecord, Grid> TO_GRID =
        RuntimeUtil.makeToRuntime(GridRecord.class, Grid.class);

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** The id of the player that owns this grid. */
    @Id public int userId;

    /** A monotonically increasing integer that is assigned to each grid owned by this player. Used
     * to ensure that the client and server are always talking about the same grid. */
    public int gridId;

    /** The current status of this grid. */
    public GridStatus status;

    /** The ids of the things in this grid. */
    public int[] thingIds;

    /** The time at which this grid expires. */
    public Timestamp expires;

    /** Generates {@link Grid#slots}. */
    public SlotStatus[] getSlots ()
    {
        return new SlotStatus[thingIds.length]; // the caller will fill this in
    }

    /** Generates {@link Grid#flipped}. */
    public ThingCard[] getFlipped ()
    {
        return new ThingCard[thingIds.length]; // the caller will fill this in
    }

    /** Generates {@link Grid#unflipped}. */
    public int[] getUnflipped ()
    {
        return new int[Rarity.values().length]; // the caller will fill this in
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GridRecord}
     * with the supplied key values.
     */
    public static Key<GridRecord> getKey (int userId)
    {
        return newKey(_R, userId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID); }
    // AUTO-GENERATED: METHODS END
}
