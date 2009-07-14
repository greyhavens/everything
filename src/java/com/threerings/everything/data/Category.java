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

    /** The creator of this category. */
    public PlayerName creator;

    /** True if this category is active (and hence items in it are usable in the game). */
    public boolean active;

    /** The number of things in this category (only valid for leaf categories). */
    public int things;

    /**
     * Returns an HTML snippet that will display cat -> cat -> cat for the supplied categories.
     */
    public static String getHierarchyHTML (Category[] cats)
    {
        StringBuilder buf = new StringBuilder();
        for (Category cat : cats) {
            if (buf.length() > 0) {
                buf.append(" &#8594; "); // right arrow
            }
            buf.append(cat.name);
        }
        return buf.toString();
    }

    // from interface Created
    public int getCreatorId ()
    {
        return creator.userId;
    }

    // from interface Comparable<Category>
    public int compareTo (Category other)
    {
        return name.compareTo(other.name);
    }
}
