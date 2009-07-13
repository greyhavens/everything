//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains info on a friend who may or may not need a particular card.
 */
public class FriendCardInfo
    implements IsSerializable, Comparable<FriendCardInfo>
{
    /** The friend in question. */
    public PlayerName friend;

    /** The number of things in the same series as this thing that the friend has. */
    public int hasThings;

    /** True if this thing is on the friend's wishlist. */
    public boolean onWishlist;

    // from interface FriendCardInfo
    public int compareTo (FriendCardInfo other)
    {
        if (hasThings != other.hasThings) {
            return (hasThings > other.hasThings) ? -1 : 1;
        } else {
            return friend.name.compareTo(other.friend.name);
        }
    }
}
