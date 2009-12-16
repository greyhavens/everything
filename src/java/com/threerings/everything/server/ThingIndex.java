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
import com.google.common.collect.Iterators;
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
import com.samskivert.util.Tuple;

import com.threerings.everything.data.Rarity;
import com.threerings.everything.server.persist.AttractorRecord;
import com.threerings.everything.server.persist.ThingInfoRecord;

import static com.threerings.everything.Log.log;

/**
 * Contains a brief summary of all things in the repository. Used for selecting cards randomly and
 * properly accounting for their rarity.
 */
public class ThingIndex
{
    public ThingIndex (
        IntIntMap catmap, Iterable<ThingInfoRecord> things, Iterable<AttractorRecord> attractors)
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
        _things.shuffle();
        for (ThingList rareList : _byrare.values()) {
            rareList.shuffle();
        }

        for (AttractorRecord attractor : attractors) {
            ThingInfo info = _byid.get(attractor.thingId);
            if (info == null) {
                log.warning("Dropping attractor for inactive thing", "thingId", attractor.thingId);
                continue;
            }
            _attractorThings.add(info);
            _attractors.put(attractor.thingId, Tuple.newTuple(attractor.title, attractor.message));
        }

        log.info("Updated things index",
            "things", _things.size(), "tweight", _things.totalWeight());
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
        ThingList rareList = _byrare.get(rarity);
        ThingList selected = rareList.copyWith(fromIds);
        if (selected.size() > 0) {
            log.debug("Picking rare thing from selection", "rarity", rarity,
                      "selected", selected);
            into.add(pickWeightedThing(selected));
        } else {
            log.debug("Picking rare thing from full set", "rarity", rarity);
            into.add(pickWeightedThing(rareList));
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
        IntSet into = new ArrayIntSet();
        // pick a rare gift from a category they collect
        ThingList things = getCategoryThings(ownedCats, Rarity.MIN_GIFT_RARITY);
        if (things.size() >= 1) {
            selectThings(things, 1, heldRares, into);
        }

        // if that didn't work, pick a random rarity X item
        if (into.isEmpty()) {
            pickThingOf(Rarity.X, new ArrayIntSet(), into);
        }

        // cope if we've got nothing
        return into.isEmpty() ? 0 : into.interator().nextInt();
    }

    /**
     * Pick a thing to use as a recruitment gift from among the specified things.
     */
    public IntSet pickRecruitmentThings (IntSet playerThingIds)
    {
        IntSet giftIds = new ArrayIntSet();
        // try 10 times to pick a Rarity II or less fully-random thing
        for (int ii = 0; ii < 10; ii++) {
            int thingId = pickWeightedThing(_things);
            if (getRarity(thingId).ordinal() <= Rarity.II.ordinal()) {
                giftIds.add(thingId);
                break;
            }
        }
        // then select up to 2 more from the player's collection
        int count = Math.min(2, playerThingIds.size() - giftIds.size());
        if (count > 0) {
            selectThings(getThings(playerThingIds), count, giftIds, giftIds);
        }
        return giftIds;
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

    /**
     * Pick an attractor card.
     * TODO: clean up tuple mess?
     */
    public Tuple<Integer, Tuple<String, String>> pickAttractor ()
    {
        if (_attractorThings.size() == 0) {
            return null;
        }
        ArrayIntSet pick = new ArrayIntSet(1);
        selectThings(_attractorThings, 1, pick, pick);
        if (pick.isEmpty()) {
            return null;
        }
        int thingId = pick.interator().nextInt();
        return Tuple.newTuple(thingId, _attractors.get(thingId));
    }

    public boolean isAttractor (int thingId)
    {
        return _attractors.containsKey(thingId);
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
        if ((excludeIds.size() / (float)things.size()) >= EXCLUDE_COPY_THRESHOLD) {
            things = things.copyWithout(excludeIds);
            excludeIds = new ArrayIntSet();
        }

        // select the requested number of random things
        int iters = 0, added = 0;
        while (added < count) {
            if (iters++ >= MAX_SELECT_ITERS) {
                // let's only log a warning if we were trying to select more than 3
                if (count > 3) {
                    log.warning("Failed to select things",
                        "setSize", things.size(), "excludedSize", excludeIds.size(),
                        "count", count, "added", added, "into.size()", into.size(),
                        new Exception("DON'T PANIC"));
                }
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
        int rando = _rando.nextInt(things.totalWeight());
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
        public void add (ThingInfo info) {
            _things.add(info);
            _totalWeight += info.rarity.weight();
        }

        public int totalWeight () {
            return _totalWeight;
        }

        public void shuffle () {
            Collections.shuffle(_things);
        }

        public int size () {
            return _things.size();
        }

        // from Iterable
        public Iterator<ThingInfo> iterator () {
            return Iterators.unmodifiableIterator(_things.iterator());
        }

        @Override
        public String toString () {
            return _things.toString();
        }

        /**
         * Create a copy of this ThingList with the specified excluded items removed.
         */
        public ThingList copyWithout (IntSet withoutIds)
        {
            if (withoutIds.isEmpty()) {
                return this;
            }
            ThingList that = new ThingList();
            for (ThingInfo info : this) {
                if (!withoutIds.contains(info.thingId)) {
                    that.add(info);
                }
            }
            return that;
        }

        public ThingList copyWith (IntSet withIds)
        {
            ThingList that = new ThingList();
            if (!withIds.isEmpty()) { // optimize for common case
                for (ThingInfo info : this) {
                    if (withIds.contains(info.thingId)) {
                        that.add(info);
                    }
                }
            }
            return that;
        }

        protected int _totalWeight;
        protected List<ThingInfo> _things = Lists.newArrayList();
    }

    protected ThingList _things = new ThingList();
    protected IntMap<ThingInfo> _byid = IntMaps.newHashIntMap();
    protected Multimap<Integer, ThingInfo> _bycat = ArrayListMultimap.create();
    protected Map<Rarity, ThingList> _byrare = Maps.newEnumMap(Rarity.class);
    protected Random _rando = new Random();
    protected IntMap<Tuple<String,String>> _attractors = IntMaps.newHashIntMap();
    protected ThingList _attractorThings = new ThingList();

    { // initialize the _byrare map
        for (Rarity rarity : Rarity.BONUS) {
            _byrare.put(rarity, new ThingList());
        }
    }

    /** Used to avoid infinite loopage. */
    protected static final int MAX_SELECT_ITERS = 1024;

    /** If we're selecting things and excluding more than this percentage of things, make a copy. */
    protected static final float EXCLUDE_COPY_THRESHOLD = 0.5f;
}
