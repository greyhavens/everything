//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * Contains full details about a particular player.
 */
public class Player
    implements Serializable
{
    /** Contains information on a player's flags. */
    public enum Flag {
        /** Indicates that this player has an extra daily free flip. */
        EXTRA_FLIP(0),

        /** Indicates that this player has logged in from the mobile client. */
        PLAYED_MOBILE(1);

        /** Returns the bitmask associated with this flag. */
        public int getMask () {
            return _mask;
        }

        Flag (int bit) {
            _mask = 1 << bit;
        }

        protected int _mask;
    };

    /** This player's name. */
    public PlayerName name;

    /** The day of year on which this player's birthday falls as MMDD. */
    public int birthdate;

    /** This player's preferred timezone. */
    public String timezone;

    /** This player's editor status. */
    public boolean isEditor;

    /** The date and time at which this player joined. */
    public Date joined;

    /** This date and time of this player's last session. */
    public Date lastSession;

    /** This player's coin balance. */
    public int coins;

    /** This player's current free flips status. */
    public int freeFlips;

    /** This player's activated flags. */
    public Set<Flag> flags;
}
