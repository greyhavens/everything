//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains data for a category or sub-category.
 */
public class Category
    implements Created, IsSerializable, Comparable<Category>
{
    /** The maximum length of a category name. */
    public static final int MAX_NAME_LENGTH = 64;

    /** The unique id for this category. */
    public int categoryId;

    /** The name of this category. */
    public String name;

    /** The id of this category's parent, if it is a sub-category, or 0. */
    public int parentId;

    /** True if this category is active (and hence items in it are usable in the game). */
    public boolean active;

    /** The id of the creator of this category. */
    public int creatorId;

    // from interface Created
    public int getCreatorId ()
    {
        return creatorId;
    }

    // from interface Comparable<Category>
    public int compareTo (Category other)
    {
        return name.compareTo(other.name);
    }
}
