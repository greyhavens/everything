//
// $Id$

package com.threerings.everything.server;

import java.util.Map;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.everything.data.Category;
import com.threerings.everything.server.persist.ThingRepository;
import com.threerings.everything.server.persist.PlayerRepository;

import com.threerings.everything.util.LazyExpiringMemoizingSupplier;

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
        return _indexSupplier.get();
    }

    /**
     * Creates a new thing index by scanning the entire thing database. This is expensive.
     */
    protected ThingIndex createThingIndex ()
    {
        Map<Integer, Integer> catmap = Maps.newHashMap();
        for (Category cat : _thingRepo.loadAllCategories()) {
            if (cat.parentId != 0) {
                catmap.put(cat.categoryId, cat.parentId);
            }
        }
        return new ThingIndex(catmap, _thingRepo.loadActiveThings(), _thingRepo.loadAttractors());
    }

    /** Supplies ThingIndex. */
    protected final Supplier<ThingIndex> _indexSupplier =
        new LazyExpiringMemoizingSupplier(
            new Supplier<ThingIndex>() {
                public ThingIndex get () {
                    return createThingIndex();
                }
            }, 5, TimeUnit.MINUTES);

    @Inject protected ThingRepository _thingRepo;
}
