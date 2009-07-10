//
// $Id$

package com.threerings.everything.server;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

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
        ArrayIntSet tmpset = new ArrayIntSet();
        for (ThingInfoRecord thing : things) {
            ThingInfo info = new ThingInfo();
            info.thingId = thing.thingId;
            info.weight = thing.rarity.weight();
            _totalWeight += info.weight;
            // resolve the categories of which this thing is a member
            int categoryId = thing.categoryId;
            while (categoryId != 0) {
                tmpset.add(categoryId);
                categoryId = catmap.getOrElse(categoryId, 0);
            }
            info.categoryIds = tmpset.toIntArray();
            _things.add(info);
        }
        // finally shuffle our things to avoid aliasing if the RNG is not perfect
        Collections.shuffle(_things);

        log.info("Updated things index", "things", _things.size(), "tweight", _totalWeight);
    }

    // TODO: support category or other limitations on thing selection

    /**
     * Selects the specified number of things from the index weighted properly according to their
     * rarity.
     */
    public int[] selectThings (int count)
    {
        Preconditions.checkArgument(_things.size() >= count,
                                    "Cannot select " + count + " things. " +
                                    "Index only contains " + _things.size() + " things.");

        // select the requested number of random things
        int iters = 0;
        ArrayIntSet things = new ArrayIntSet();
        while (things.size() < count) {
            if (iters++ >= MAX_SELECT_ITERS) {
                throw new RuntimeException("Failed to select " + count + " things after " +
                                           MAX_SELECT_ITERS + " attempts.");
            }
            things.add(pickWeightedThing());
        }

        // shuffle the thing ids for maximum randosity
        int[] thingIds = things.toIntArray();
        ArrayUtil.shuffle(thingIds, _rando);
        return thingIds;
    }

    protected int pickWeightedThing ()
    {
        int rando = _rando.nextInt(_totalWeight);
        for (int ii = 0, ll = _things.size(); ii < ll; ii++) {
            ThingInfo info = _things.get(ii);
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
        public int[] categoryIds;
    }

    protected List<ThingInfo> _things = Lists.newArrayList();
    protected int _totalWeight;
    protected Random _rando = new Random();

    /** Used to avoid infinite loopage. */
    protected static final int MAX_SELECT_ITERS = 1024;
}
