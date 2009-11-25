//
// $Id$

package com.threerings.everything.tests;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import org.junit.*;
import static org.junit.Assert.*;

import com.threerings.everything.data.Rarity;
import com.threerings.everything.server.ThingIndex;
import com.threerings.everything.server.persist.AttractorRecord;
import com.threerings.everything.server.persist.ThingInfoRecord;

/**
 * Test the thing index.
 */
public class ThingIndexTest
{
    @Test public void testPickBonus ()
    {
        ThingIndex index = createTestIndex();
        for (Rarity rarity : Rarity.BONUS) {
            IntSet into = new ArrayIntSet();
            index.pickThingOf(rarity, new ArrayIntSet(), into);
            assertTrue(into.size() > 0);
        }
    }

    @Test public void testPickBirthdayThing ()
    {
        IntMap<ThingInfoRecord> things = createThings();
        List<AttractorRecord> attractors = Collections.emptyList();
        ThingIndex index = new ThingIndex(mapCategories(things), things.values(), attractors);
        // create a random collection
        IntSet ids = new ArrayIntSet(), exclude = new ArrayIntSet();
        index.selectThings(25, ids, exclude);
        // compute owned categories and held rares
        IntSet ownedCats = new ArrayIntSet(), heldRares = new ArrayIntSet();
        for (int thingId : ids) {
            ThingInfoRecord info = things.get(thingId);
            ownedCats.add(info.categoryId);
            if (Rarity.MIN_GIFT_RARITY.ordinal() <= info.rarity.ordinal()) {
                heldRares.add(info.thingId);
            }
        }
        for (int ii = 0; ii < 25; ii++) {
            int thingId = index.pickBirthdayThing(ownedCats, heldRares);
            ThingInfoRecord info = things.get(thingId);
            // System.err.println("Picked " + thingId + " of " + info.rarity);
            assertTrue(info.rarity.ordinal() >= Rarity.MIN_GIFT_RARITY.ordinal());
        }
    }

    protected ThingIndex createTestIndex ()
    {
        IntMap<ThingInfoRecord> things = createThings();
        List<AttractorRecord> attractors = Collections.emptyList();
        return new ThingIndex(mapCategories(things), things.values(), attractors);
    }

    protected IntMap<ThingInfoRecord> createThings ()
    {
        IntMap<ThingInfoRecord> things = IntMaps.newHashIntMap();
        for (int ii = 0; ii < 25; ii++) {
            addCategory(things);
        }
        return things;
    }

    protected IntIntMap mapCategories (IntMap<ThingInfoRecord> things)
    {
        IntIntMap catmap = new IntIntMap();
        for (ThingInfoRecord thing : things.values()) {
            catmap.put(thing.categoryId, thing.categoryId/10);
            catmap.put(thing.categoryId/10, thing.categoryId/100);
        }
        return catmap;
    }

    protected void addCategory (IntMap<ThingInfoRecord> things)
    {
        int categoryId = ++_nextCategoryId;
        List<Rarity> series;
        switch (categoryId % 3) {
        default:
        case 0: series = SMALL_SERIES; break;
        case 1: series = MEDIUM_SERIES; break;
        case 2: series = LARGE_SERIES; break;
        }
        for (Rarity rarity : series) {
            ThingInfoRecord thing = createThing(categoryId, rarity);
            things.put(thing.thingId, thing);
        }
    }

    protected ThingInfoRecord createThing (int categoryId, Rarity rarity)
    {
        ThingInfoRecord record = new ThingInfoRecord();
        record.thingId = ++_nextThingId;
        record.categoryId = categoryId;
        record.rarity = rarity;
        return record;
    }

    protected int _nextCategoryId = 100, _nextThingId;

    protected static final List<Rarity> SMALL_SERIES = Lists.newArrayList(
        Rarity.I, Rarity.I, Rarity.II, Rarity.II, Rarity.III, Rarity.IV, Rarity.V, Rarity.VIII);
    protected static final List<Rarity> MEDIUM_SERIES = Lists.newArrayList(
        Rarity.I, Rarity.I, Rarity.I, Rarity.II, Rarity.II, Rarity.III, Rarity.III, Rarity.IV,
        Rarity.V, Rarity.VI, Rarity.VIII, Rarity.X);
    protected static final List<Rarity> LARGE_SERIES = Lists.newArrayList(
        Rarity.I, Rarity.I, Rarity.I, Rarity.II, Rarity.II, Rarity.II, Rarity.III, Rarity.III,
        Rarity.IV, Rarity.V, Rarity.VI, Rarity.VI, Rarity.VII, Rarity.VIII, Rarity.IX, Rarity.X);
}
