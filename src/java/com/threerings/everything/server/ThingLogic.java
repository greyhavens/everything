//
// $Id$

package com.threerings.everything.server;

import java.util.Map;
import java.util.Set;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.server.persist.ThingRepository;
import com.threerings.everything.server.persist.PlayerRepository;

import com.threerings.everything.util.LazyExpiringMemoizingSupplier;

/**
 * Thing related server logic.
 */
@Singleton
public class ThingLogic
{
    /** Twice the normal weight. */
    public static final Float LIKE_WEIGHT = 2f;

    /** The minimum weighting such that every card still has a nonzero chance of appearing. */
    public static final Float DISLIKE_WEIGHT = 1f / Rarity.X.weight();

    /** Converts a (-1 : 1) likability into a (DISLIKE_WEIGHT : LIKE_WEIGHT) weighting. */
    public static final Function<Float, Float> LIKABILITY_TO_WEIGHT =
        new Function<Float, Float>() {
            public Float apply (Float likability)
            {
                if (likability >= 0f) {
                    return 1 + (likability * (LIKE_WEIGHT - 1));

                } else {
                    return 1 - (-likability * (1 - DISLIKE_WEIGHT));
                }
            }
        };

    /**
     * Returns a recently computed thing index. May block if the index needs updating.
     */
    public ThingIndex getThingIndex ()
    {
        return _indexSupplier.get();
    }

    /**
     * Get the global likes for each series, expressed as a value from -1 to 1.
     */
    public Map<Integer, Float> getGlobalLikes ()
    {
        return _likeSupplier.get();
    }

    /**
     * Get the weightings computed from the global likes.
     */
    public Map<Integer, Float> getGlobalWeights ()
    {
        // go ahead and copy to a new one...
        return Maps.newHashMap(Maps.transformValues(getGlobalLikes(), LIKABILITY_TO_WEIGHT));
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
        new LazyExpiringMemoizingSupplier<ThingIndex>(
            new Supplier<ThingIndex>() {
                public ThingIndex get () {
                    return createThingIndex();
                }
            }, 5, TimeUnit.MINUTES);

    /** Supplies weights. */
    protected final Supplier<Map<Integer, Float>> _likeSupplier =
        new LazyExpiringMemoizingSupplier<Map<Integer, Float>>(
            new Supplier<Map<Integer, Float>>() {
                public Map<Integer, Float> get () {
                    return _playerRepo.loadGlobalLikes();
                }
            }, 18, TimeUnit.MINUTES);

    @Inject protected ThingRepository _thingRepo;
    @Inject protected PlayerRepository _playerRepo;
}
