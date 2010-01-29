//
// $Id$

package com.threerings.everything.server;

import java.util.Map;
import java.util.Set;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.TrophyData;
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
     * Get the info on valid trophies.
     */
    public Map<Set<Integer>, TrophyData> getTrophies ()
    {
        // TODO: this will eventually probably be loaded from the db
        return _trophies;
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

    /** Trophies */
    protected final Map<Set<Integer>, TrophyData> _trophies =
        new ImmutableMap.Builder<Set<Integer>, TrophyData>()
        .put(ImmutableSet.of(311, 315, 322, 332),
            new TrophyData("presidents", "U.S. Presidents", "Collect all U.S. Presidents"))
        .put(ImmutableSet.of(430, 432, 434),
            new TrophyData("carnivore", "Carnivore", "Collect all the cuts of meat"))
        .put(ImmutableSet.of(154, 155, 156, 157, 158, 159, 160),
            new TrophyData("consoles", "Game Consoles", "Collect every generation of game console"))
        .put(ImmutableSet.of(184, 188, 189, 199, 205, 211, 273),
            new TrophyData("sevens", "Sevens", "Collect all 'Seven' series"))
        .put(ImmutableSet.of(350, 351, 352, 353),
            new TrophyData("us_states", "All 50 States", "Collect every US State"))
        .put(ImmutableSet.of(17, 98, 181, 235, 249, 257, 285, 289, 306, 355, 357, 362),
            new TrophyData("mammals", "Mammals", "Collect all the mammals"))
        // and so on....
//        .put(ImmutableSet.of(232),
//            new TrophyData("seasons", "Seasons", "This is a fake for testing"))
        .build();

    @Inject protected ThingRepository _thingRepo;
    @Inject protected PlayerRepository _playerRepo;
}
