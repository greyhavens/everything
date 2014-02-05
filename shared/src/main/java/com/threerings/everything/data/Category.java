//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.io.Serializable;
import java.util.Map;

import com.samskivert.util.ByteEnum;

/**
 * Contains data for a category or sub-category.
 */
public class Category
    implements Created, Serializable, Comparable<Category>
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

    /** The unicode character for the category/subcat/series separator. */
    public static final String SEP_CHAR = "\u2023";

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
    public static String getHierarchy (Category[] cats) {
        StringBuilder buf = new StringBuilder();
        for (Category cat : cats) {
            if (buf.length() > 0) {
                buf.append(" ").append(SEP_CHAR).append(" ");
            }
            buf.append(cat.name);
        }
        return buf.toString();
    }

    /**
     * Returns an HTML snippet that will display cat -> cat -> cat for the supplied leaf category,
     * resolving its parent categories via {@code parents}.
     */
    public static String getHierarchy (Category leaf, Map<Integer,Category> parents) {
        StringBuilder buf = new StringBuilder();
        addCategory(buf, leaf, parents);
        return buf.toString();
    }
    private static void addCategory (StringBuilder buf, Category cat, Map<Integer,Category> pars) {
        Category par = pars.get(cat.parentId);
        if (par != null) {
            addCategory(buf, par, pars);
            buf.append(" ").append(SEP_CHAR).append(" ");
        }
        buf.append(cat.name);
    }

    /**
     * Returns true if this category is in development, false if not.
     */
    public boolean isInDevelopment () {
        return state == State.IN_DEVELOPMENT;
    }

    /**
     * Returns true if this category is active, false if not.
     */
    public boolean isActive () {
        return state == State.ACTIVE;
    }

    // from interface Created
    public int getCreatorId () {
        return creator.userId;
    }

    // from interface Comparable<Category>
    public int compareTo (Category other) {
        return name.compareTo(other.name);
    }

    @Override public String toString () {
        return name;
    }
}
