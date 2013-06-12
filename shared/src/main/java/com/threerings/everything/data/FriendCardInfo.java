//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.io.Serializable;

/**
 * Contains info on a friend who may or may not need a particular card.
 */
public class FriendCardInfo
    implements Serializable, Comparable<FriendCardInfo>
{
    /** The friend in question. */
    public PlayerName friend;

    /** The number of things in the same series as this thing that the friend has. */
    public int hasThings;

    /** The friend's like preference for this thing's category, if any. */
    public Boolean like;

    // from interface FriendCardInfo
    public int compareTo (FriendCardInfo other)
    {
        int cmp = likeComparison(like) - likeComparison(other.like);
        if (cmp == 0) {
            cmp = other.hasThings - hasThings;
            if (cmp == 0) {
                cmp = friend.name.compareTo(other.friend.name);
            }
        }
        return cmp;
//        return ComparisonChain.start()
//            .compare(likeComparison(like), likeComparison(other.like))
//            .compare(other.hasThings, hasThings)
//            .compare(friend.name, other.friend.name)
//            .result();
    }

    /**
     * Return what the 'like' value compares as.
     * We want the following ordering: like, no-pref, dislike.
     */
    protected static int likeComparison (Boolean like)
    {
        return (like == null) ? 0 : (like ? -1 : 1);
    }
}
