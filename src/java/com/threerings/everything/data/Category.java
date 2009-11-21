//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.samskivert.util.ByteEnum;

/**
 * Contains data for a category or sub-category.
 */
public class Category
    implements Created, IsSerializable, Comparable<Category>
{
    /** This category's activation state. */
    public enum State implements ByteEnum {
        IN_DEVELOPMENT(0), PENDING_REVIEW(1), ACTIVE(2);

        // from ByteEnum
        public byte toByte () {
            return _code;
        }

        State (int code) {
            _code = (byte)code;
        }

        protected byte _code;
    }

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
    protected boolean active;

    /** The activation state of this category. */
    public State state;

    /** The number of things in this category (only valid for leaf categories). */
    public int things;

    /** The number of things in this series for which the creator has been paid. */
    public int paid;

    /**
     * Returns an HTML snippet that will display cat -> cat -> cat for the supplied categories.
     */
    public static String getHierarchy (Category[] cats)
    {
        StringBuilder buf = new StringBuilder();
        for (Category cat : cats) {
            if (buf.length() > 0) {
                buf.append(" \u2023 ");
            }
            buf.append(cat.name);
        }
        return buf.toString();
    }

    /**
     * Returns true if this category is in development, false if not.
     */
    public boolean isInDevelopment ()
    {
        return state == State.IN_DEVELOPMENT;
    }

    /**
     * Returns true if this category is active, false if not.
     */
    public boolean isActive ()
    {
        return state == State.ACTIVE;
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

    @Override
    public String toString ()
    {
        return name;
    }
}
