//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;

/**
 * Manages player state for the Everything app.
 */
@Singleton
public class PlayerRepository extends DepotRepository
{
    @Inject public PlayerRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads the player with the specified user id. Returns null if none exists.
     */
    public PlayerRecord loadPlayer (int userId)
    {
        return load(PlayerRecord.getKey(userId));
    }

    /**
     * Creates, inserts and returns a new player record.
     */
    public PlayerRecord createPlayer (int userId, String name, long birthday)
    {
        PlayerRecord record = new PlayerRecord();
        record.userId = userId;
        record.name = name;
        record.birthday = (birthday == 0L) ? null : new Date(birthday);
        record.joined = new Timestamp(System.currentTimeMillis());
        record.lastSession = record.joined;
        insert(record);
        return record;
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(PlayerRecord.class);
        classes.add(CardRecord.class);
        classes.add(PowerupRecord.class);
        classes.add(WishRecord.class);
    }
}
