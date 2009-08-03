//
// $Id$

package com.threerings.everything.server;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

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
            info.rarity = thing.rarity;
            _totalWeight += info.rarity.weight();
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
    public void selectThings (int count, Set<Integer> into, Set<Integer> excludeIds)
    {
        log.debug("Selecting " + count + " things from entire collection " + _things.size());
        selectThings(count, _things, _totalWeight, excludeIds, into);
    }

    /**
     * Selects the specified number of things from the subset of things in the specified set of
     * categories.
     */
    public void selectThingsFrom (Set<Integer> catIds, int count, Set<Integer> into)
    {
        ThingList things = getCategoryThings(catIds, Rarity.I);
        log.debug("Selecting " + count + " things from " + catIds + " " + things.things.size());
        selectThings(count, things.things, things.totalWeight, new ArrayIntSet(), into);
    }

    /**
     * Returns one thing of the specified rarity (must be V or higher).
     */
    public int pickThingOf (Rarity rarity)
    {
        int totalWeight = 0;
        for (ThingInfo info : _byrare.get(rarity)) {
            totalWeight += info.rarity.weight();
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
                totalWeight += info.rarity.weight();
                things.add(info);
            }
        }
        Collections.shuffle(things);
        return pickWeightedThing(things, totalWeight);
    }

    /**
     * Selects a birthday thing to give to a player who is collecting the specified series.
     */
    public int pickBirthdayThing (Set<Integer> ownedCats, Set<Integer> heldRares)
    {
        ThingList things = getCategoryThings(ownedCats, Rarity.MIN_GIFT_RARITY);

        // remove the things they already have
        for (Iterator<ThingInfo> iter = things.things.iterator(); iter.hasNext(); ) {
            ThingInfo info = iter.next();
            if (heldRares.contains(info.thingId)) {
                iter.remove();
                things.totalWeight -= info.rarity.weight();
            }
        }

        // if there are any left, pick one of those as a gift
        if (things.things.size() > 0) {
            return pickWeightedThing(things);
        }

        // otherwise pick a random rarity X item as a gift
        return pickThingOf(Rarity.X);
    }

    protected ThingList getCategoryThings (Set<Integer> catIds, Rarity minRarity)
    {
        ThingList things = new ThingList();
        for (int catId : catIds) {
            for (ThingInfo info : _bycat.get(catId)) {
                if (info.rarity.ordinal() >= minRarity.ordinal()) {
                    things.things.add(info);
                    things.totalWeight += info.rarity.weight();
                }
            }
        }
        return things;
    }

    protected void selectThings (int count, List<ThingInfo> things, int totalWeight,
                                 Set<Integer> excludeIds, Set<Integer> into)
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

    protected int pickWeightedThing (ThingList things)
    {
        return pickWeightedThing(things.things, things.totalWeight);
    }

    protected int pickWeightedThing (List<ThingInfo> things, int totalWeight)
    {
        int rando = _rando.nextInt(totalWeight);
        for (ThingInfo info : things) {
            if (rando < info.rarity.weight()) {
                return info.thingId;
            }
            rando -= info.rarity.weight();
        }
        throw new IllegalStateException("Random weight exceeded total weight! So impossible!");
    }

    protected static class ThingList
    {
        public List<ThingInfo> things = Lists.newArrayList();
        public int totalWeight;
    }

    protected static class ThingInfo
    {
        public int thingId;
        public Rarity rarity;
    }

    protected List<ThingInfo> _things = Lists.newArrayList();
    protected int _totalWeight;
    protected Multimap<Integer, ThingInfo> _bycat = ArrayListMultimap.create();
    protected ArrayListMultimap<Rarity, ThingInfo> _byrare = ArrayListMultimap.create();
    protected Random _rando = new Random();

    /** Used to avoid infinite loopage. */
    protected static final int MAX_SELECT_ITERS = 1024;
}
