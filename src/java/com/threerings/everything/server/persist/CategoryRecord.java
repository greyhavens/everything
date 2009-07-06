//
// $Id$

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

import com.threerings.everything.data.Category;

/**
 * Defines a particular category or sub-category.
 */
public class CategoryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CategoryRecord> _R = CategoryRecord.class;
    public static final ColumnExp SET_ID = colexp(_R, "setId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp PARENT_ID = colexp(_R, "parentId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** A function for converting persistent records to runtime records. */
    public static Function<CategoryRecord, Category> TO_CATEGORY =
        new Function<CategoryRecord, Category>() {
        public Category apply (CategoryRecord record) {
            return record.toCategory();
        }
    };

    /** A unique identifier for this category. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int categoryId;

    @Column(length=Category.MAX_NAME_LENGTH)
    public String name;

    /** The category to which this set belongs. */
    @Index public int parentId;

    /** The id of the user that created this category. */
    public int creatorId;

    /**
     * Converts this persistent record to a runtime record.
     */
    public Category toCategory ()
    {
        Category cat = new Category();
        cat.categoryId = categoryId;
        cat.name = name;
        cat.parentId = parentId;
        return cat;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link CategoryRecord}
     * with the supplied key values.
     */
    public static Key<CategoryRecord> getKey (int setId)
    {
        return new Key<CategoryRecord>(
                CategoryRecord.class,
                new ColumnExp[] { SET_ID },
                new Comparable[] { setId });
    }
    // AUTO-GENERATED: METHODS END
}
