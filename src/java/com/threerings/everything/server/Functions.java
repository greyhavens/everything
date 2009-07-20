//
// $Id$

package com.threerings.everything.server;

import com.google.common.base.Function;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.Thing;

/**
 * Useful functions.
 */
public class Functions
{
    /** Extracts the category id from a thing. */
    public static final Function<Thing, Integer> GET_CATEGORY = new Function<Thing, Integer>() {
        public Integer apply (Thing thing) {
            return thing.categoryId;
        }
    };

    /** Extracts the parent category id from a category. */
    public static final Function<Category, Integer> GET_PARENT = new Function<Category, Integer>() {
        public Integer apply (Category category) {
            return category.parentId;
        }
    };
}
