//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server.persist;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.util.RuntimeUtil;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.Thing;
import com.threerings.everything.data.ThingCard;

/**
 * Contains data on a single thing.
 */
public class ThingRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ThingRecord> _R = ThingRecord.class;
    public static final ColumnExp<Integer> THING_ID = colexp(_R, "thingId");
    public static final ColumnExp<Integer> CATEGORY_ID = colexp(_R, "categoryId");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    public static final ColumnExp<Rarity> RARITY = colexp(_R, "rarity");
    public static final ColumnExp<String> IMAGE = colexp(_R, "image");
    public static final ColumnExp<String> DESCRIP = colexp(_R, "descrip");
    public static final ColumnExp<String> FACTS = colexp(_R, "facts");
    public static final ColumnExp<String> SOURCE = colexp(_R, "source");
    public static final ColumnExp<Integer> CREATOR_ID = colexp(_R, "creatorId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 4;

    /** A function for converting persistent records to runtime records. */
    public static Function<ThingRecord, Thing> TO_THING =
        RuntimeUtil.makeToRuntime(ThingRecord.class, Thing.class);

    /** A function for converting this record to {@link ThingCard}. */
    public static Function<ThingRecord, ThingCard> TO_CARD =
        RuntimeUtil.makeToRuntime(ThingRecord.class, ThingCard.class);

    /** A function for converting runtime records to persistent records. */
    public static Function<Thing, ThingRecord> FROM_THING =
        RuntimeUtil.makeToRecord(Thing.class, ThingRecord.class);

    /** A unique identifier for this thing. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int thingId;

    /** The (leaf) category to which this thing belongs. */
    @Index public int categoryId;

    /** The name of this thing. */
    @Column(length=Thing.MAX_NAME_LENGTH)
    public String name;

    /** This thing's rarity level. */
    public Rarity rarity;

    /** The name of this thing's image (its media hash). */
    public String image;

    /** This thing's description. */
    @Column(length=Thing.MAX_DESCRIP_LENGTH)
    public String descrip;

    /** Facts about this thing. */
    @Column(length=Thing.MAX_FACTS_LENGTH)
    public String facts;

    /** The URL from which this thing's information was obtained. */
    public String source;

    /** The id of the user that created this thing. */
    public int creatorId;

    /**
     * Converts this persistent record to a runtime record.
     */
    public Thing toThing ()
    {
        return TO_THING.apply(this);
    }

    /**
     * Initializes {@link Thing#creator}.
     */
    public PlayerName getCreator ()
    {
        return PlayerName.create(creatorId);
    }

    /**
     * Initializes {@link #creatorId}.
     */
    public void setCreator (PlayerName creator)
    {
        this.creatorId = (creator == null) ? 0 : creator.userId;
    }

    /**
     * Initializes {@link ThingCard#received} with 0L.
     */
    public long getReceived ()
    {
        return 0L;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ThingRecord}
     * with the supplied key values.
     */
    public static Key<ThingRecord> getKey (int thingId)
    {
        return newKey(_R, thingId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(THING_ID); }
    // AUTO-GENERATED: METHODS END
}
