//
// $Id$

package com.threerings.everything.server.persist;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Computed;

@Computed(shadowOf=LikeRecord.class)
public class LikeCountRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<LikeCountRecord> _R = LikeCountRecord.class;
    public static final ColumnExp CATEGORY_ID = colexp(_R, "categoryId");
    public static final ColumnExp LIKE = colexp(_R, "like");
    public static final ColumnExp COUNT = colexp(_R, "count");
    // AUTO-GENERATED: FIELDS END

    /** The categoryId. */
    public int categoryId;

    /** Liked or disliked. */
    public boolean like;

    /** How often is this category liked or disliked? */
    @Computed(fieldDefinition="count(*)")
    public int count;
}
