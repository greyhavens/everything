//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server.persist;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.util.RuntimeUtil;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.SeriesCard;

/**
 * Defines a particular category or sub-category.
 */
public class CategoryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CategoryRecord> _R = CategoryRecord.class;
    public static final ColumnExp<Integer> CATEGORY_ID = colexp(_R, "categoryId");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    public static final ColumnExp<Integer> PARENT_ID = colexp(_R, "parentId");
    public static final ColumnExp<Integer> CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp<Boolean> ACTIVE = colexp(_R, "active");
    public static final ColumnExp<Category.State> STATE = colexp(_R, "state");
    public static final ColumnExp<Integer> THINGS = colexp(_R, "things");
    public static final ColumnExp<Integer> PAID = colexp(_R, "paid");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 6;

    /** A function for converting persistent records to runtime records. */
    public static Function<CategoryRecord, Category> TO_CATEGORY =
        RuntimeUtil.makeToRuntime(CategoryRecord.class, Category.class);

    /** A function for converting persistent records to runtime records. */
    public static Function<CategoryRecord, SeriesCard> TO_SERIES_CARD =
        RuntimeUtil.makeToRuntime(CategoryRecord.class, SeriesCard.class);

    /** A function for converting runtime records to persistent records. */
    public static Function<Category, CategoryRecord> FROM_CATEGORY =
        RuntimeUtil.makeToRecord(Category.class, CategoryRecord.class);

    /** A unique identifier for this category. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int categoryId;

    @Column(length=Category.MAX_NAME_LENGTH)
    public String name;

    /** This category's parent or 0 if it is a top-level category. */
    @Index public int parentId;

    /** The id of the user that created this category. */
    @Index public int creatorId;

    /** True if this category is active (and hence items in it are usable in the game). */
    public boolean active;

    /** The activation state of this category. */
    public Category.State state;

    /** The number of things in this category (only valid for leaf categories). */
    public int things;

    /** The number of things in this series for which the creator has been paid. */
    public int paid;

    /**
     * Converts this persistent record to a runtime record.
     */
    public Category toCategory ()
    {
        return TO_CATEGORY.apply(this);
    }

    /**
     * Initializes {@link Category#creator}.
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
     * Fills in 0 for {@link SeriesCard#owned}. Caller actually populates.
     */
    public int getOwned ()
    {
        return 0;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link CategoryRecord}
     * with the supplied key values.
     */
    public static Key<CategoryRecord> getKey (int categoryId)
    {
        return newKey(_R, categoryId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(CATEGORY_ID); }
    // AUTO-GENERATED: METHODS END
}
