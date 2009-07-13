//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.util.RuntimeUtil;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.PlayerName;

/**
 * Records an action take by a player that we report to other players to keep them abreast of fun
 * happenings.
 */
public class FeedItemRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FeedItemRecord> _R = FeedItemRecord.class;
    public static final ColumnExp ACTOR_ID = colexp(_R, "actorId");
    public static final ColumnExp WHEN = colexp(_R, "when");
    public static final ColumnExp TYPE = colexp(_R, "type");
    public static final ColumnExp TARGET_ID = colexp(_R, "targetId");
    public static final ColumnExp OBJECT = colexp(_R, "object");
    // AUTO-GENERATED: FIELDS END

    /** A function for converting persistent records to runtime records. */
    public static Function<FeedItemRecord, FeedItem> TO_FEED_ITEM =
        RuntimeUtil.makeToRuntime(FeedItemRecord.class, FeedItem.class);

    /** The id of user that took the action. */
    @Id public int actorId;

    /** The time at which the action was taken. */
    @Id public Timestamp when;

    /** The type of action taken. */
    public FeedItem.Type type;

    /** The the player toward whom this action was targeted (or 0). */
    public int targetId;

    /** A textual description of the object of the action. */
    public String object;

    /**
     * Initializes {@link FeedItem#actor}.
     */
    public PlayerName getActor ()
    {
        return PlayerName.create(actorId);
    }

    /**
     * Initializes {@link FeedItem#target}.
     */
    public PlayerName getTarget ()
    {
        return (targetId == 0) ? null : PlayerName.create(targetId);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FeedItemRecord}
     * with the supplied key values.
     */
    public static Key<FeedItemRecord> getKey (int actorId, Timestamp when)
    {
        return new Key<FeedItemRecord>(
                FeedItemRecord.class,
                new ColumnExp[] { ACTOR_ID, WHEN },
                new Comparable[] { actorId, when });
    }
    // AUTO-GENERATED: METHODS END
}
