//
// $Id$

package com.threerings.everything.server.persist;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;

/**
 * Manages persistent state for the Everything app.
 */
@Singleton
public class EverythingRepository extends DepotRepository
{
    @Inject public EverythingRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CardRecord.class);
        classes.add(PlayerRecord.class);
        classes.add(PowerupRecord.class);
        classes.add(SetRecord.class);
        classes.add(ThingRecord.class);
        classes.add(WishRecord.class);
    }
}
