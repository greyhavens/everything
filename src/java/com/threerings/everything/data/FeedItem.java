//
// $Id$

package com.threerings.everything.data;

import java.util.Date;
import java.util.List;

import com.samskivert.depot.ByteEnum;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a player action, for display in the "recent happenings" feed.
 */
public class FeedItem
    implements IsSerializable
{
    /** Identifies the different types of feed items. */
    public enum Type implements ByteEnum {
        FLIPPED(0), GIFTED(1);

        // from ByteEnum
        public byte toByte () {
            return _code;
        }

        Type (int code) {
            _code = (byte)code;
        }

        protected byte _code;
    };

    /** The user that took the action. */
    public PlayerName actor;

    /** The time at which the action was taken. */
    public Date when;

    /** The type of action taken. */
    public Type type;

    /** The the player toward whom this action was targeted (or null). */
    public PlayerName target;

    /** A textual description of the object(s) of the action. */
    public List<String> objects;
}
