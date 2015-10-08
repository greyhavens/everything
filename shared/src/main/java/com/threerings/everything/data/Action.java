//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.data;

import java.io.Serializable;
import java.util.Date;

import com.samskivert.util.ByteEnum;

/**
 * Records an action taken by an editor or admin on the database.
 */
public class Action
    implements Serializable
{
    /** Identifies the different targets of actions. */
    public enum Target implements ByteEnum {
        NONE(0), PLAYER(1), CATEGORY(2), THING(3);

        // from ByteEnum
        public byte toByte () {
            return _code;
        }

        Target (int code) {
            _code = (byte)code;
        }

        protected byte _code;
    };

    /** The user that took the action. */
    public int userId;

    /** The time at which the action was taken. */
    public Date when;

    /** The type of the target of the action. */
    public Target target;

    /** The unique id of the target of the action. */
    public int targetId;

    /** A textual description of the action. */
    public String action;
}
