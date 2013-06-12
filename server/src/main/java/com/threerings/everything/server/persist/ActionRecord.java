//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.util.RuntimeUtil;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Id;

import com.threerings.everything.data.Action;

/**
 * Records an action taken by an editor or admin on the database.
 */
public class ActionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ActionRecord> _R = ActionRecord.class;
    public static final ColumnExp<Integer> USER_ID = colexp(_R, "userId");
    public static final ColumnExp<Timestamp> WHEN = colexp(_R, "when");
    public static final ColumnExp<Action.Target> TARGET = colexp(_R, "target");
    public static final ColumnExp<Integer> TARGET_ID = colexp(_R, "targetId");
    public static final ColumnExp<String> ACTION = colexp(_R, "action");
    // AUTO-GENERATED: FIELDS END

    /** A function for converting persistent records to runtime records. */
    public static Function<ActionRecord, Action> TO_ACTION =
        RuntimeUtil.makeToRuntime(ActionRecord.class, Action.class);

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The user that took the action. */
    @Id public int userId;

    /** The time at which the action was taken. */
    @Id public Timestamp when;

    /** The type of the target of the action. */
    public Action.Target target;

    /** The unique id of the target of the action. */
    public int targetId;

    /** A textual description of the action. */
    public String action;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ActionRecord}
     * with the supplied key values.
     */
    public static Key<ActionRecord> getKey (int userId, Timestamp when)
    {
        return newKey(_R, userId, when);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(USER_ID, WHEN); }
    // AUTO-GENERATED: METHODS END
}
