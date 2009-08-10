//
// $Id$

package com.threerings.everything.server;

import com.google.common.base.Function;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.Thing;
import com.threerings.everything.server.persist.CardRecord;

/**
 * Useful functions.
 */
public class EFuncs
{
    /** Extracts the thing id from a thing. */
    public static final Function<Thing, Integer> THING_ID = new Function<Thing, Integer>() {
        public Integer apply (Thing thing) {
            return thing.thingId;
        }
    };

    /** Extracts the category id from a thing. */
    public static final Function<Thing, Integer> CATEGORY_ID = new Function<Thing, Integer>() {
        public Integer apply (Thing thing) {
            return thing.categoryId;
        }
    };

    /** Extracts the parent category id from a category. */
    public static final Function<Category, Integer> PARENT_ID = new Function<Category, Integer>() {
        public Integer apply (Category category) {
            return category.parentId;
        }
    };

    /** Extracts the thing id from a card. */
    public static final Function<CardRecord, Integer> CARD_THING_ID =
        new Function<CardRecord, Integer>() {
        public Integer apply (CardRecord card) {
            return card.thingId;
        }
    };
}
