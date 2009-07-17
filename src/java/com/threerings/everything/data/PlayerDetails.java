//
// $Id$

package com.threerings.everything.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains details about a particular player, only visible to admins.
 */
public class PlayerDetails
    implements IsSerializable
{
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
}
