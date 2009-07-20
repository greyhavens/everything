//
// $Id$

package com.threerings.everything.data;

import java.util.Date;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains full details about a particular player.
 */
public class Player
    implements IsSerializable
{
    /** Contains information on a player's flags. */
    public enum Flag {
        /** Indicates that this player has an extra daily free flip. */
        EXTRA_FLIP(0);

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

    /** This player's birthday. */
    public Date birthday;

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
