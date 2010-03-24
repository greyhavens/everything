//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.util.RuntimeUtil;

import com.threerings.everything.data.CategoryComment;
import com.threerings.everything.data.PlayerName;

/**
 * Tracks comments made on a category by editors.
 */
public class CategoryCommentRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CategoryCommentRecord> _R = CategoryCommentRecord.class;
    public static final ColumnExp CATEGORY_ID = colexp(_R, "categoryId");
    public static final ColumnExp WHEN = colexp(_R, "when");
    public static final ColumnExp COMMENTOR_ID = colexp(_R, "commentorId");
    public static final ColumnExp MESSAGE = colexp(_R, "message");
    // AUTO-GENERATED: FIELDS END

    /** A function for converting persistent records to runtime records. */
    public static Function<CategoryCommentRecord, CategoryComment> TO_COMMENT =
        RuntimeUtil.makeToRuntime(CategoryCommentRecord.class, CategoryComment.class);

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The category on which the comment was made. */
    @Id public int categoryId;

    /** The time at which the action was taken. */
    @Id public Timestamp when;

    /** The id of the commentor. */
    public int commentorId;

    /** The text of the comment. */
    public String message;

    /**
     * Initializes {@link CategoryComment#commentor}.
     */
    public PlayerName getCommentor ()
    {
        return PlayerName.create(commentorId); // rest filled in by caller
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link CategoryCommentRecord}
     * with the supplied key values.
     */
    public static Key<CategoryCommentRecord> getKey (int categoryId, Timestamp when)
    {
        return newKey(_R, categoryId, when);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(CATEGORY_ID, WHEN); }
    // AUTO-GENERATED: METHODS END
}
