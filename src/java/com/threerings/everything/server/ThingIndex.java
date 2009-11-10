//
// $Id$

package com.threerings.everything.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Interator;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;

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
            info.categoryId = thing.categoryId;
            info.rarity = thing.rarity;
            // resolve the categories of which this thing is a member
            int categoryId = thing.categoryId;
            while (categoryId != 0) {
                _bycat.put(categoryId, info);
                categoryId = catmap.getOrElse(categoryId, 0);
            }
            if (Rarity.BONUS.contains(thing.rarity)) {
                _byrare.get(thing.rarity).add(info);
            }
            _things.add(info);
            _byid.put(info.thingId, info);
        }

        // finally shuffle our things to avoid aliasing if the RNG is not perfect
        Collections.shuffle(_things.things);
        for (Rarity rare : Rarity.BONUS) {
            Collections.shuffle(_byrare.get(rare).things);
        }

        log.info("Updated things index", "things", _things.size(), "tweight", _things.totalWeight);
    }

    // TODO: support category or other limitations on thing selection

    /**
     * Returns the category to which the specified thing belongs or 0 if the thing is unknown.
     */
    public int getCategory (int thingId)
    {
        ThingInfo info = _byid.get(thingId);
        return (info == null) ? 0 : info.categoryId;
    }

    /**
     * Returns the rarity of the specified thing or null if the thing is unknown.
     */
    public Rarity getRarity (int thingId)
    {
        ThingInfo info = _byid.get(thingId);
        return (info == null) ? null : info.rarity;
    }

    /**
     * Returns the number of cards in the specified category.
     */
    public int getCategorySize (int categoryId)
    {
        return _bycat.get(categoryId).size();
    }

    /**
     * Computes the set of all needed thing ids given the supplied collection. Needed is a card in
     * a series the player is collecting but do not have.
     */
    public IntSet computeNeeded (Multimap<Integer, Integer> collection)
    {
        IntSet needed = new ArrayIntSet();
        for (Map.Entry<Integer, Collection<Integer>> entry : collection.asMap().entrySet()) {
            int catId = entry.getKey();
            Collection<Integer> heldIds = entry.getValue();
            for (ThingInfo info : _bycat.get(catId)) {
                if (!heldIds.contains(info.thingId)) {
                    needed.add(info.thingId);
                }
            }
        }
        return needed;
    }

    /**
     * Selects the specified number of things from the index weighted properly according to their
     * rarity. Things in the supplied exclusion set will not be chosen.
     */
    public void selectThings (int count, IntSet excludeIds, IntSet into)
    {
        log.debug("Selecting " + count + " things from entire collection " + _things.size());
        selectThings(_things, count, excludeIds, into);
    }

    /**
     * Selects the specified number of things from the subset of things in the specified set of
     * categories.
     */
    public void selectThingsFrom (IntSet catIds, int count, IntSet into)
    {
        selectThingsFrom(catIds, count, new ArrayIntSet(), into);
    }

    /**
     * Selects the specified number of things from the subset of things in the specified set of
     * categories, excluding any things in the supplied exclusion set.
     */
    public void selectThingsFrom (IntSet catIds, int count, IntSet excludeIds, IntSet into)
    {
        ThingList things = getCategoryThings(catIds, Rarity.I);
        log.debug("Selecting " + count + " things from " + catIds, "things", things.size(),
                  "excluding", excludeIds);
        selectThings(things, count, excludeIds, into);
    }

    /**
     * Returns one thing of the specified rarity (must be V or higher). If possible, the thing will
     * be selected from those in the supplied set.
     */
    public void pickThingOf (Rarity rarity, IntSet fromIds, IntSet into)
    {
        ThingList selected = new ThingList();
        for (ThingInfo info : _byrare.get(rarity)) {
            if (fromIds.contains(info.thingId)) {
                selected.add(info);
            }
        }
        if (selected.size() > 0) {
            log.debug("Picking rare thing from selection", "rarity", rarity,
                      "selected", selected);
            into.add(pickWeightedThing(selected));
        } else {
            log.debug("Picking rare thing from full set", "rarity", rarity);
            into.add(pickWeightedThing(_byrare.get(rarity)));
        }
    }

    /**
     * Returns one thing of at least the specified rarity (must be V or higher) and which also must
     * be in the contained set of ids.
     */
    public void pickBonusThing (IntSet fromIds, IntSet into)
    {
        ThingList things = new ThingList();
        for (Rarity rare : Rarity.BONUS) {
            for (ThingInfo info : _byrare.get(rare)) {
                if (fromIds.contains(info.thingId)) {
                    things.add(info);
                }
            }
        }
        if (things.size() > 0) {
            into.add(pickWeightedThing(things));
        }
    }

    /**
     * Selects a birthday thing to give to a player who is collecting the specified series.
     */
    public int pickBirthdayThing (IntSet ownedCats, IntSet heldRares)
    {
        ThingList things = getCategoryThings(ownedCats, Rarity.MIN_GIFT_RARITY);

        // remove the things they already have
        for (Iterator<ThingInfo> iter = things.iterator(); iter.hasNext(); ) {
            ThingInfo info = iter.next();
            if (heldRares.contains(info.thingId)) {
                iter.remove();
                things.totalWeight -= info.rarity.weight();
            }
        }

        // if there are any left, pick one of those as a gift
        if (things.size() > 0) {
            return pickWeightedThing(things);
        }

        // otherwise pick a random rarity X item as a gift
        IntSet into = new ArrayIntSet();
        pickThingOf(Rarity.X, new ArrayIntSet(), into);
        return into.isEmpty() ? 0 : into.interator().nextInt();
    }

    /**
     * Pick a thing to use as a recruitment gift from among the specified things.
     */
    public int pickRecruitmentThing (IntSet thingIds)
    {
        if (thingIds.isEmpty()) {
            return 0;
        }
        return pickWeightedThing(getThings(thingIds));
    }

    /**
     * Selects a seed item from the specified seed category for a new player.
     */
    public int pickSeedThing (int seedCat)
    {
        ThingList things = new ThingList();
        for (ThingInfo info : _bycat.get(seedCat)) {
            if (info.rarity == Rarity.I) {
                things.add(info);
            }
        }
        return (things.size() == 0) ? 0 : pickWeightedThing(things);
    }

    protected ThingList getThings (IntSet thingIds)
    {
        ThingList things = new ThingList();
        for (Interator it = thingIds.interator(); it.hasNext();) {
            things.add(_byid.get(it.nextInt()));
        }
        return things;
    }

    protected ThingList getCategoryThings (IntSet catIds, Rarity minRarity)
    {
        ThingList things = new ThingList();
        for (int catId : catIds) {
            for (ThingInfo info : _bycat.get(catId)) {
                if (info.rarity.ordinal() >= minRarity.ordinal()) {
                    things.add(info);
                }
            }
        }
        return things;
    }

    protected void selectThings (ThingList things, int count, IntSet excludeIds, IntSet into)
    {
        Preconditions.checkArgument(things.size() >= count,
                                    "Cannot select " + count + " things. " +
                                    "Index only contains " + things.size() + " things.");

        // select the requested number of random things
        int iters = 0, added = 0;
        while (added < count) {
            if (iters++ >= MAX_SELECT_ITERS) {
                log.warning("Failed to select " + count + " things after " +
                            MAX_SELECT_ITERS + " attempts.");
                break;
            }
            int thingId = pickWeightedThing(things);
            if (!excludeIds.contains(thingId) && into.add(thingId)) {
                added++;
            }
        }
    }

    protected int pickWeightedThing (ThingList things)
    {
        int rando = _rando.nextInt(things.totalWeight);
        for (ThingInfo info : things) {
            rando -= info.rarity.weight();
            if (rando < 0) {
                return info.thingId;
            }
        }
        throw new IllegalStateException("Random weight exceeded total weight! So impossible!");
    }

    protected static class ThingInfo
    {
        public int thingId;
        public int categoryId;
        public Rarity rarity;

        public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    protected static class ThingList
        implements Iterable<ThingInfo>
    {
        public List<ThingInfo> things = Lists.newArrayList();
        public int totalWeight;

        public void add (ThingInfo info) {
            things.add(info);
            totalWeight += info.rarity.weight();
        }

        public int size () {
            return things.size();
        }

        // from Iterable
        public Iterator<ThingInfo> iterator () {
            return things.iterator();
        }

        @Override
        public String toString () {
            return things.toString();
        }
    }

    protected ThingList _things = new ThingList();
    protected IntMap<ThingInfo> _byid = IntMaps.newHashIntMap();
    protected Multimap<Integer, ThingInfo> _bycat = ArrayListMultimap.create();
    protected Map<Rarity, ThingList> _byrare = Maps.newEnumMap(Rarity.class);
    protected Random _rando = new Random();

    { // initialize the _byrare map
        for (Rarity rarity : Rarity.BONUS) {
            _byrare.put(rarity, new ThingList());
        }
    }

    /** Used to avoid infinite loopage. */
    protected static final int MAX_SELECT_ITERS = 1024;
}
