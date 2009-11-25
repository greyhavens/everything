//
// $Id$

package com.threerings.everything.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntIntMap;

import com.threerings.everything.data.Category;
import com.threerings.everything.server.persist.ThingRepository;

/**
 * Thing related server logic.
 */
@Singleton
public class ThingLogic
{
    /**
     * Returns a recently computed thing index. May block if the index needs updating.
     */
    public ThingIndex getThingIndex ()
    {
        long now = System.currentTimeMillis();
        synchronized (this) {
            if (_index != null && now < _nextIndexUpdate) {
                return _index;
            }
            _nextIndexUpdate = now + THING_INDEX_UPDATE_INTERVAL;
            // before the first thing index is generated, all callers must block
            if (_index == null) {
                return (_index = createThingIndex());
            }
            // otherwise other callers can use the old index until we've generated the new one
        }

        ThingIndex index = createThingIndex();
        synchronized (this) {
            _index = index;
        }
        return index;
    }

    /**
     * Creates a new thing index by scanning the entire thing database. This is expensive.
     */
    protected ThingIndex createThingIndex ()
    {
        IntIntMap catmap = new IntIntMap();
        for (Category cat : _thingRepo.loadAllCategories()) {
            if (cat.parentId != 0) {
                catmap.put(cat.categoryId, cat.parentId);
            }
        }
        return new ThingIndex(catmap, _thingRepo.loadActiveThings(), _thingRepo.loadAttractors());
    }

    protected ThingIndex _index;
    protected long _nextIndexUpdate;

    @Inject protected ThingRepository _thingRepo;

    /** Recompute our thing index every five minutes. */
    protected static final long THING_INDEX_UPDATE_INTERVAL = 5 * 60 * 1000L;
}
