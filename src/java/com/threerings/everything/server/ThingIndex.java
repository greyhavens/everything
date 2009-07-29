//
// $Id$

package com.threerings.everything.server;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntSet;

import com.threerings.everything.data.Rarity;
import com.threerings.everything.server.persist.ThingInfoRecord;

import static com.threerings.everything.Log.log;

/**
 * Contains a brief summary of all things in the repository. Used for selecting cards randomly and
 * properly accounting for their rarity.
 */
public class ThingIndex
{
    public ThingIndex (IntIntMap catmap, Iterable<ThingInfoRecord> things)
    {
        for (ThingInfoRecord thing : things) {
            ThingInfo info = new ThingInfo();
            info.thingId = thing.thingId;
            info.weight = thing.rarity.weight();
            _totalWeight += info.weight;
            // resolve the categories of which this thing is a member
            int categoryId = thing.categoryId;
            while (categoryId != 0) {
                _bycat.put(categoryId, info);
                categoryId = catmap.getOrElse(categoryId, 0);
            }
            if (Rarity.BONUS.contains(thing.rarity)) {
                _byrare.put(thing.rarity, info);
            }
            _things.add(info);
        }

        // finally shuffle our things to avoid aliasing if the RNG is not perfect
        Collections.shuffle(_things);
        for (Rarity rare : Rarity.BONUS) {
            Collections.shuffle(_byrare.get(rare));
        }

        log.info("Updated things index", "things", _things.size(), "tweight", _totalWeight);
    }

    // TODO: support category or other limitations on thing selection

    /**
     * Returns the number of cards in the specified category.
     */
    public int getCategorySize (int categoryId)
    {
        return _bycat.get(categoryId).size();
    }

    /**
     * Selects the specified number of things from the index weighted properly according to their
     * rarity. Things in the supplied exclusion set will not be chosen.
     */
    public void selectThings (int count, IntSet into, IntSet excludeIds)
    {
        log.debug("Selecting " + count + " things from entire collection " + _things.size());
        selectThings(count, _things, _totalWeight, excludeIds, into);
    }

    /**
     * Selects the specified number of things from the subset of things in the specified set of
     * categories.
     */
    public void selectThingsFrom (IntSet catIds, int count, IntSet into)
    {
        List<ThingInfo> things = Lists.newArrayList();
        int totalWeight = 0;
        for (int catId : catIds) {
            for (ThingInfo info : _bycat.get(catId)) {
                things.add(info);
                totalWeight += info.weight;
            }
        }
        log.debug("Selecting " + count + " things from " + catIds + " " + things.size());
        selectThings(count, things, totalWeight, new ArrayIntSet(), into);
    }

    /**
     * Returns one thing of the specified rarity (must be V or higher).
     */
    public int pickThingOf (Rarity rarity)
    {
        int totalWeight = 0;
        for (ThingInfo info : _byrare.get(rarity)) {
            totalWeight += info.weight;
        }
        return pickWeightedThing(_byrare.get(rarity), totalWeight);
    }

    /**
     * Returns one thing of at least the specified rarity (must be V or higher).
     */
    public int pickBonusThing ()
    {
        int totalWeight = 0;
        List<ThingInfo> things = Lists.newArrayList();
        for (Rarity rare : Rarity.BONUS) {
            for (ThingInfo info : _byrare.get(rare)) {
                totalWeight += info.weight;
                things.add(info);
            }
        }
        Collections.shuffle(things);
        return pickWeightedThing(things, totalWeight);
    }

    protected void selectThings (int count, List<ThingInfo> things, int totalWeight,
                                 IntSet excludeIds, IntSet into)
    {
        Preconditions.checkArgument(things.size() >= count,
                                    "Cannot select " + count + " things. " +
                                    "Index only contains " + things.size() + " things.");

        // select the requested number of random things
        int iters = 0, added = 0;
        while (added < count) {
            if (iters++ >= MAX_SELECT_ITERS) {
                throw new RuntimeException("Failed to select " + count + " things after " +
                                           MAX_SELECT_ITERS + " attempts.");
            }
            int thingId = pickWeightedThing(things, totalWeight);
            if (!excludeIds.contains(thingId) && into.add(thingId)) {
                added++;
            }
        }
    }

    protected int pickWeightedThing (List<ThingInfo> things, int totalWeight)
    {
        int rando = _rando.nextInt(totalWeight);
        for (ThingInfo info : things) {
            if (rando < info.weight) {
                return info.thingId;
            }
            rando -= info.weight;
        }
        throw new IllegalStateException("Random weight exceeded total weight! So impossible!");
    }

    protected static class ThingInfo
    {
        public int thingId;
        public int weight;
    }

    protected List<ThingInfo> _things = Lists.newArrayList();
    protected int _totalWeight;
    protected Multimap<Integer, ThingInfo> _bycat = ArrayListMultimap.create();
    protected ArrayListMultimap<Rarity, ThingInfo> _byrare = ArrayListMultimap.create();
    protected Random _rando = new Random();

    /** Used to avoid infinite loopage. */
    protected static final int MAX_SELECT_ITERS = 1024;
}
