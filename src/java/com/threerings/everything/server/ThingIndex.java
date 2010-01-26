//
// $Id$

package com.threerings.everything.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import com.samskivert.util.StringUtil;

import com.threerings.everything.data.Rarity;
import com.threerings.everything.server.persist.AttractorRecord;
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
        Map<Integer, Integer> catmap, Iterable<ThingInfoRecord> things,
        Iterable<AttractorRecord> attractors)
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

        for (AttractorRecord attractor : attractors) {
            ThingInfo info = _byid.get(attractor.thingId);
            if (info == null) {
                log.warning("Dropping attractor for inactive thing", "thingId", attractor.thingId);
                continue;
            }
            _attractorThings.add(info);
            _attractors.put(attractor.thingId, attractor.toInfo());
        }

        log.info("Updated things index",
            "things", _things.size(), "tweight", _things.totalWeight());
    }

    // TODO: support category or other limitations on thing selection

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
        copy._byid = Maps.newHashMap();
        for (Map.Entry<Integer, ThingInfo> entry : _byid.entrySet()) {
            copy._byid.put(entry.getKey(), entry.getValue().copyWeighted(weights));
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
    public Set<Integer> computeNeeded (Multimap<Integer, Integer> collection)
    {
        Set<Integer> needed = Sets.newHashSet();
        for (Map.Entry<Integer, Collection<Integer>> entry : collection.asMap().entrySet()) {
            int catId = entry.getKey();
            ThingList catList = _bycat.get(catId);
            if (catList != null) {
                Collection<Integer> heldIds = entry.getValue();
                for (ThingInfo info : catList) {
                    if (!heldIds.contains(info.thingId)) {
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
    public void selectThings (int count, Set<Integer> excludeIds, Set<Integer> into)
    {
        log.debug("Selecting " + count + " things from entire collection " + _things.size());
        selectThings(_things, count, excludeIds, into);
    }

    /**
     * Selects the specified number of things from the subset of things in the specified set of
     * categories.
     */
    public void selectThingsFrom (Set<Integer> catIds, int count, Set<Integer> into)
    {
        selectThingsFrom(catIds, count, Sets.<Integer>newHashSet(), into);
    }

    /**
     * Selects the specified number of things from the subset of things in the specified set of
     * categories, excluding any things in the supplied exclusion set.
     */
    public void selectThingsFrom (
        Set<Integer> catIds, int count, Set<Integer> excludeIds, Set<Integer> into)
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
    public void pickThingOf (Rarity rarity, Set<Integer> fromIds, Set<Integer> into)
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
        ThingList things = getCategoryThings(ownedCats, Rarity.MIN_GIFT_RARITY);
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
    public Set<Integer> pickRecruitmentThings (Set<Integer> playerThingIds)
    {
        Set<Integer> giftIds = Sets.newHashSet();
        // try 10 times to pick a Rarity II or less fully-random thing
        for (int ii = 0; ii < 10; ii++) {
            int thingId = pickWeightedThing(_things);
            if (getRarity(thingId).ordinal() <= Rarity.II.ordinal()) {
                giftIds.add(thingId);
                break;
            }
        }
        // then select up to 1 or 2 more from the player's collection
        int count = Math.min(1 + _rando.nextInt(2), playerThingIds.size() - giftIds.size());
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

    /**
     * Pick an attractor card.
     */
    public AttractorInfo pickAttractor (Map<Integer, Float> weights)
    {
        ThingList atts = _attractorThings.copyWeighted(weights);
        if (atts.size() == 0) {
            return null;
        }
        Set<Integer> pick = Sets.newHashSet();
        selectThings(atts, 1, pick, pick);
        if (pick.isEmpty()) {
            return null;
        }
        return _attractors.get(pick.iterator().next());
    }

    public boolean isAttractor (int thingId)
    {
        return _attractors.containsKey(thingId);
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

    protected ThingList getThings (Set<Integer> thingIds)
    {
        ThingList things = new ThingList();
        for (Iterator it = thingIds.iterator(); it.hasNext();) {
            things.add(_byid.get(it.next()));
        }
        return things;
    }

    protected ThingList getCategoryThings (Set<Integer> catIds, Rarity minRarity)
    {
        ThingList things = new ThingList();
        for (int catId : catIds) {
            ThingList catList = _bycat.get(catId);
            if (catList != null) {
                for (ThingInfo info : catList) {
                    if (info.rarity.ordinal() >= minRarity.ordinal()) {
                        things.add(info);
                    }
                }
            }
        }
        return things;
    }

    /**
     * Select things from the specified list.
     */
    protected void selectThings (
        ThingList things, int count, Set<Integer> excludeIds, Set<Integer> into)
    {
        selectThings(things, count, excludeIds, into, false);
    }

    /**
     * Select things from the specified list, optionally forcing the list to copy itself
     * excluding the ids already in excludeIds or into.
     */
    protected void selectThings (
        ThingList things, int count, Set<Integer> excludeIds, Set<Integer> into, boolean forceCopy)
    {
        if (forceCopy ||
                (((excludeIds.size() + into.size()) / (float)things.size()) >=
                     EXCLUDE_COPY_THRESHOLD)) {
            // make a copy without the stuff we've already excluded or selected
            things = things.copyWithout(excludeIds).copyWithout(into);
            excludeIds = Sets.newHashSet();
        }

        // if the thing list is now empty, there's no point in continuing
        if (things.size() == 0) {
            log.info("Failed to select things", "desired", count, "into.size()", into.size(),
                new Exception("DON'T PANIC"));
            return;
        }

        // select the requested number of random things
        for (int added = 0, missed = 0; added < count; ) {
            int thingId = pickWeightedThing(things);
            if (!excludeIds.contains(thingId) && into.add(thingId)) {
                added++;

            } else if (++missed >= MAX_SELECT_ITERS) {
                // we're spinning our wheels trying to pick something, let's try again
                // with a copy of the ThingList that excludes stuff we can't use
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
    }

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

        protected int _totalWeight;
        protected List<ThingInfo> _things = Lists.newArrayList();
    }

    protected ThingList _things = new ThingList();
    protected Map<Integer, ThingInfo> _byid = Maps.newHashMap();
    protected Map<Integer, ThingList> _bycat = Maps.newHashMap();
    protected Map<Rarity, ThingList> _byrare = Maps.newEnumMap(Rarity.class);
    protected Random _rando = new Random();

    /** Attractors are never stored weighted, but weighted when used. */
    protected Map<Integer, AttractorInfo> _attractors = Maps.newHashMap();
    protected ThingList _attractorThings = new ThingList();

    { // initialize the _byrare map
        for (Rarity rarity : Rarity.BONUS) {
            _byrare.put(rarity, new ThingList());
        }
    }

    /** Used to avoid infinite loopage. */
    protected static final int MAX_SELECT_ITERS = 256;

    /** If we're selecting things and excluding more than this percentage of things, make a copy. */
    protected static final float EXCLUDE_COPY_THRESHOLD = 0.5f;
}
