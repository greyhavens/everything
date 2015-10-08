//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server.persist;

import java.sql.Timestamp;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;

import com.threerings.everything.data.Action;

/**
 * Manages admin persistent data.
 */
@Singleton
public class AdminRepository extends DepotRepository
{
    @Inject public AdminRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Records an action to the repository.
     */
    public void recordAction (int userId, Action.Target target, int targetId, String action)
    {
        ActionRecord record = new ActionRecord();
        record.userId = userId;
        record.when = new Timestamp(System.currentTimeMillis());
        record.target = target;
        record.targetId = targetId;
        record.action = action;
        insert(record);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ActionRecord.class);
    }
}
