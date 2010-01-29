//
// $Id$

package com.threerings.everything.data;

import java.util.Date;
import java.util.List;

import com.samskivert.util.ByteEnum;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a player action, for display in the "recent happenings" feed.
 */
public class FeedItem
    implements IsSerializable, Comparable<FeedItem>
{
    /** Identifies the different types of feed items. */
    public enum Type implements ByteEnum {
        FLIPPED(0), NOTUSED(1), GOTGIFT(2), COMPLETED(3), NEW_SERIES(4), BIRTHDAY(5), JOINED(6),
        TROPHY(7);

        // from ByteEnum
        public byte toByte () {
            return _code;
        }

        Type (int code) {
            _code = (byte)code;
        }

        protected byte _code;
    };

    /** The subject of the feed item. */
    public PlayerName actor;

    /** The time at which the action was taken. */
    public Date when;

    /** The type of action taken. */
    public Type type;

    /** The indirect object of the action (e.g. a player from whom a gift was received) or null. */
    public PlayerName target;

    /** A textual description of the object(s) of the action. */
    public List<String> objects;

    // from interface Comparable<FeedItem>
    public int compareTo (FeedItem other)
    {
        return other.when.compareTo(when); // most recent to least
    }
}
