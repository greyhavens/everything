//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import com.samskivert.util.StringUtil;

import com.threerings.everything.data.Rarity;
import com.threerings.everything.server.persist.LikeRecord;
import com.threerings.everything.server.persist.ThingInfoRecord;

import static com.threerings.everything.Log.log;

/**
 * Contains a brief summary of all things in the repository. Used for selecting cards randomly and
 * properly accounting for their rarity.
 */
public class ThingIndex
    implements Cloneable
{
    public ThingIndex (
        Map<Integer, Integer> catmap, Iterable<ThingInfoRecord> things)
    {
        for (ThingInfoRecord thing : things) {
            ThingInfo info = new ThingInfo(thing);
            // resolve the categories of which this thing is a member
            int categoryId = thing.categoryId;
            while (categoryId != 0) {
                ThingList catList = _bycat.get(categoryId);
                if (catList == null) {
                    _bycat.put(categoryId, catList = new ThingList());
                }
                catList.add(info);
                Integer parentId = catmap.get(categoryId);
                categoryId = (parentId == null) ? 0 : parentId;
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
        log.info("Updated things index", "things", _things.size(), "tweight", _things.totalWeight());
    }

    /**
     * Make a copy of this ThingIndex, with category weightings applied.
     *
     * @param weights a mapping of categoryId -> weighting adjustment. Categories not specified
     * will not be adjusted.
     */
    public ThingIndex copyWeighted (Map<Integer, Float> weights)
    {
        if (weights.isEmpty()) {
            return this;
        }
        ThingIndex copy = clone();
        copy._things = _things.copyWeighted(weights);
        copy._byrare = Maps.newEnumMap(Rarity.class);
        for (Map.Entry<Rarity, ThingList> entry : _byrare.entrySet()) {
            copy._byrare.put(entry.getKey(), entry.getValue().copyWeighted(weights));
        }
        copy._bycat = Maps.newHashMap();
        for (Map.Entry<Integer, ThingList> entry : _bycat.entrySet()) {
            copy._bycat.put(entry.getKey(), entry.getValue().copyWeighted(weights));
        }
        return copy;
    }

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
        ThingList catList = _bycat.get(categoryId);
        return (catList == null) ? 0 : catList.size();
    }

    /**
     * Computes the set of all needed thing ids given the supplied collection. Needed is a card in
     * a series the player is collecting but do not have.
     */
    public Set<Integer> computeNeeded (Multimap<Integer, Integer> collection, Rarity maxRarity)
    {
        int maxR = maxRarity.ordinal();
        Set<Integer> needed = Sets.newHashSet();
        for (Map.Entry<Integer, Collection<Integer>> entry : collection.asMap().entrySet()) {
            int catId = entry.getKey();
            ThingList catList = _bycat.get(catId);
            if (catList != null) {
                Collection<Integer> heldIds = entry.getValue();
                for (ThingInfo info : catList) {
                    if (!heldIds.contains(info.thingId) && info.rarity.ordinal() <= maxR) {
                        needed.add(info.thingId);
                    }
                }
            }
        }
        return needed;
    }

    /**
     * Selects the specified number of things from the index weighted properly according to their
     * rarity. Things in the supplied exclusion set will not be chosen.
     */
    public void selectThings (int count, Rarity maxRarity, Set<Integer> excludeIds,
                              Set<Integer> into)
    {
        log.debug("Selecting " + count + " things from entire collection " + _things.size());
        selectThings(_things.copyWithRarityCap(maxRarity), count, excludeIds, into);
    }

    /**
     * Selects the specified number of things from the subset of things in the specified set of
     * categories.
     */
    public void selectThingsFrom (Set<Integer> catIds, int count, Rarity maxRarity,
                                  Set<Integer> into)
    {
        selectThingsFrom(catIds, count, maxRarity, Collections.<Integer>emptySet(), into);
    }

    /**
     * Selects the specified number of things from the subset of things in the specified set of
     * categories, excluding any things in the supplied exclusion set.
     */
    public void selectThingsFrom (Set<Integer> catIds, int count, Rarity maxRarity,
                                  Set<Integer> excludeIds, Set<Integer> into)
    {
        ThingList things = getCategoryThings(catIds, Rarity.I, maxRarity);
        log.debug("Selecting " + count + " things from " + catIds, "things", things.size(),
                  "excluding", excludeIds);
        selectThings(things, count, excludeIds, into);
    }

    /**
     * Returns one thing of the specified rarity (must be V or higher). If possible, the thing will
     * be selected from those in the supplied set.
     */
    public void pickThingOf (Rarity rarity, Set<Integer> fromIds, Set<Integer> into)
    {
        ThingList rareList = _byrare.get(rarity);
        ThingList selected = rareList.copyWith(fromIds);
        if (selected.size() > 0) {
            log.debug("Picking rare thing from selection", "rarity", rarity, "selected", selected);
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
    public void pickBonusThing (Set<Integer> fromIds, Set<Integer> into)
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
    public int pickBirthdayThing (Set<Integer> ownedCats, Set<Integer> heldRares)
    {
        Set<Integer> into = Sets.newHashSet();
        // pick a rare gift from a category they collect
        ThingList things = getCategoryThings(ownedCats, Rarity.MIN_GIFT_RARITY, Rarity.X);
        if (things.size() >= 1) {
            selectThings(things, 1, heldRares, into);
        }

        // if that didn't work, pick a random rarity X item
        if (into.isEmpty()) {
            pickThingOf(Rarity.X, Sets.<Integer>newHashSet(), into);
        }

        // cope if we've got nothing
        return into.isEmpty() ? 0 : into.iterator().next();
    }

    /**
     * Pick a thing to use as a recruitment gift from among the specified things.
     */
    public Set<Integer> pickRecruitmentThings (List<LikeRecord> likes)
    {
        Set<Integer> disliked = Sets.newHashSet();
        Set<Integer> liked = Sets.newHashSet();
        for (LikeRecord rec : likes) {
            (rec.like ? liked : disliked).add(rec.categoryId);
        }

        // if they have at least 5 'like' categories, select from only those, else just exclude
        // their dislikes
        ThingList things = (liked.size() >= 5)
            ? _things.copyWith(catsToThings(liked))
            : _things.copyWithout(catsToThings(disliked));

        if (things.size() == 0) {
            return Collections.emptySet(); // what the hell? They dislike everything?
        }

        int count = 1 + _rando.nextInt(3);
        Set<Integer> giftIds = Sets.newHashSet();
        for (int pick = 0; pick < count; pick++) {
            // try 10 times to pick something Rarity II or less
            for (int ii = 0; ii < 10; ii++) {
                int thingId = pickWeightedThing(things);
                if (getRarity(thingId).ordinal() <= Rarity.II.ordinal()) {
                    giftIds.add(thingId);
                    break;
                }
            }
        }
        return giftIds;
    }

    /**
     * Selects a seed item from the specified seed category for a new player.
     */
    public int pickSeedThing (int seedCat)
    {
        ThingList catList = _bycat.get(seedCat);
        if (catList == null) {
            return 0;
        }
        ThingList things = new ThingList();
        for (ThingInfo info : catList) {
            if (info.rarity == Rarity.I) {
                things.add(info);
            }
        }
        return (things.size() == 0) ? 0 : pickWeightedThing(things);
    }

    @Override // "from" Cloneable
    public ThingIndex clone ()
    {
        try {
            ThingIndex copy = (ThingIndex) super.clone();
            copy._rando = new Random(_rando.nextLong());
            return copy;

        } catch (CloneNotSupportedException cnse) {
            throw new AssertionError(cnse);
        }
    }

    /**
     * Convert the categoryIds to the thingIds contained in all of them.
     */
    protected Set<Integer> catsToThings (Set<Integer> catIds)
    {
        if (catIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Integer> thingIds = Sets.newHashSet();
        for (Integer catId : catIds) {
            for (ThingInfo info : _bycat.get(catId)) {
                thingIds.add(info.thingId);
            }
        }
        return thingIds;
    }

    protected ThingList getCategoryThings (Set<Integer> catIds, Rarity minRarity, Rarity maxRarity)
    {
        int minR = minRarity.ordinal(), maxR = maxRarity.ordinal();
        ThingList things = new ThingList();
        for (int catId : catIds) {
            ThingList catList = _bycat.get(catId);
            if (catList != null) {
                for (ThingInfo info : catList) {
                    int r = info.rarity.ordinal();
                    if (r < minR || r > maxR) continue;
                    things.add(info);
                }
            }
        }
        return things;
    }

    /**
     * Select things from the specified list.
     */
    protected void selectThings (ThingList things, int count, Set<Integer> excludeIds,
                                 Set<Integer> into)
    {
        selectThings(things, count, excludeIds, into, false);
    }

    /**
     * Select things from the specified list, optionally forcing the list to copy itself excluding
     * the ids already in excludeIds or into.
     */
    protected void selectThings (ThingList things, int count, Set<Integer> excludeIds,
                                 Set<Integer> into, boolean forceCopy)
    {
        if (forceCopy ||
            (((excludeIds.size() + into.size()) / (float)things.size()) >= EXCLUDE_COPY_THRESHOLD)) {
            // make a copy without the stuff we've already excluded or selected
            things = things.copyWithout(excludeIds).copyWithout(into);
            excludeIds = Collections.emptySet();
        }

        // if the thing list is now empty, there's no point in continuing
        if (things.size() == 0) {
            log.warning("Failed to select things (usually non-critical)", "desired", count,
                        "into.size()", into.size());
            return;
        }

        // select the requested number of random things
        for (int added = 0, missed = 0; added < count; ) {
            int thingId = pickWeightedThing(things);
            if (!excludeIds.contains(thingId) && into.add(thingId)) added++;
            else if (++missed >= MAX_SELECT_ITERS) {
                if (added == 0) {
                    log.warning("Spinning while not selecting things!", "forceCopy", forceCopy);
                    if (forceCopy) return; // oh, that's bad
                }
                // we're spinning our wheels trying to pick something, let's try again with a copy
                // of the ThingList that excludes stuff we can't use
                selectThings(things, count - added, excludeIds, into, true);
                return;
            }
        }
    }

    protected int pickWeightedThing (ThingList things)
    {
        int rando = _rando.nextInt(things.totalWeight());
        for (ThingInfo info : things) {
            rando -= info.weight;
            if (rando < 0) {
                return info.thingId;
            }
        }
        throw new IllegalStateException("Random weight exceeded total weight! So impossible!");
    }

    protected static class ThingInfo
    {
        public final int thingId;
        public final int categoryId;
        public final Rarity rarity;
        public final int weight;

        public ThingInfo (ThingInfoRecord record)
        {
            thingId = record.thingId;
            categoryId = record.categoryId;
            rarity = record.rarity;
            weight = rarity.weight();
        }

        protected ThingInfo (ThingInfo info, float adjust)
        {
            thingId = info.thingId;
            categoryId = info.categoryId;
            rarity = info.rarity;
            weight = Math.round(rarity.weight() * adjust);
        }

        public ThingInfo copyWeighted (Map<Integer, Float> weights)
        {
            Float adjust = weights.get(categoryId);
            return ((adjust == null) || (adjust == 1f)) ? this : new ThingInfo(this, adjust);
        }

        public String toString () {
            return StringUtil.fieldsToString(this);
        }
    } // end: class ThingInfo

    protected static class ThingList
        implements Iterable<ThingInfo>
    {
        public void add (ThingInfo info)
        {
            if (info.weight > 0) {
                _things.add(info);
                _totalWeight += info.weight;
            }
        }

        public int totalWeight ()
        {
            return _totalWeight;
        }

        public void shuffle ()
        {
            Collections.shuffle(_things);
        }

        public int size ()
        {
            return _things.size();
        }

        // from Iterable
        public Iterator<ThingInfo> iterator ()
        {
            return Iterators.unmodifiableIterator(_things.iterator());
        }

        @Override
        public String toString ()
        {
            return _things.toString();
        }

        /**
         * Create a copy of this ThingList with the specified excluded items removed.
         */
        public ThingList copyWithout (Set<Integer> withoutIds)
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

        public ThingList copyWith (Set<Integer> withIds)
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

        public ThingList copyWeighted (Map<Integer, Float> weights)
        {
            if (weights.isEmpty()) {
                return this;
            }
            ThingList that = new ThingList();
            for (ThingInfo info : this) {
                that.add(info.copyWeighted(weights));
            }
            return that;
        }

        public ThingList copyWithRarityCap (Rarity maxRarity) {
            if (maxRarity == Rarity.X) {
                return this;
            }
            int maxR = maxRarity.ordinal();
            ThingList that = new ThingList();
            for (ThingInfo info : this) {
                if (info.rarity.ordinal() <= maxR) {
                    that.add(info);
                }
            }
            return that;
        }

        protected int _totalWeight;
        protected List<ThingInfo> _things = Lists.newArrayList();
    } // end: class ThingList

    /** The complete list of things. May be weighted. */
    protected ThingList _things = new ThingList();

    /** Things by thingId. Never weighted. */
    protected Map<Integer, ThingInfo> _byid = Maps.newHashMap();

    /** ThingLists by categoryId. May be weighted. */
    protected Map<Integer, ThingList> _bycat = Maps.newHashMap();

    /** ThingLists by rarity (BONUS rarities only). May be weighted. */
    protected Map<Rarity, ThingList> _byrare = Maps.newEnumMap(Rarity.class);
    { // initialize the _byrare map
        for (Rarity rarity : Rarity.BONUS) {
            _byrare.put(rarity, new ThingList());
        }
    }

    /** Used for picking random things. */
    protected Random _rando = new Random();

    /** Used to avoid infinite loopage. */
    protected static final int MAX_SELECT_ITERS = 256;

    /** If we're selecting things and excluding more than this percentage of things, make a copy. */
    protected static final float EXCLUDE_COPY_THRESHOLD = 0.5f;
}
