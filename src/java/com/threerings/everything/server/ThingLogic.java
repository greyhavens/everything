//
// $Id$

package com.threerings.everything.server;

import java.util.Map;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
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
     * Get the global weights for each series, as determined by the entire user population's
     * likes/dislikes.
     */
    public Map<Integer, Float> getGlobalWeights ()
    {
        return _weightSupplier.get();
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

    /**
     * Creates a new global weights map.
     */
    protected Map<Integer, Float> createGlobalWeights ()
    {
        Map<Integer, int[]> likes = _playerRepo.loadGlobalLikes();
        // first, let's figure out the maximum number of votes on any particular issue
        int maxVotes = 0;
        for (int[] like : likes.values()) {
            maxVotes = Math.max(maxVotes, like[0] + like[1]);
        }

        // now create a map that scales from 2 to 0, centered on 1, for each category
        ImmutableMap.Builder<Integer, Float> builder = ImmutableMap.builder();
        for (Map.Entry<Integer, int[]> entry : likes.entrySet()) {
            int[] like = entry.getValue();
            float weight = 1 + ((like[0] - like[1]) / (float)maxVotes);
            builder.put(entry.getKey(), weight);
        }
        return builder.build();
    }

    /** Supplies ThingIndex. */
    protected final Supplier<ThingIndex> _indexSupplier =
        new LazyExpiringMemoizingSupplier<ThingIndex>(
            new Supplier<ThingIndex>() {
                public ThingIndex get () {
                    return createThingIndex();
                }
            }, 5, TimeUnit.MINUTES);

    /** Supplies weights. */
    protected final Supplier<Map<Integer, Float>> _weightSupplier =
        new LazyExpiringMemoizingSupplier<Map<Integer, Float>>(
            new Supplier<Map<Integer, Float>>() {
                public Map<Integer, Float> get () {
                    return createGlobalWeights();
                }
            }, 18, TimeUnit.MINUTES);

    @Inject protected ThingRepository _thingRepo;
    @Inject protected PlayerRepository _playerRepo;
}
