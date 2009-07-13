//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A player's id and name.
 */
public class PlayerName
    implements IsSerializable
{
    /** This player's unique id. */
    public int userId;

    /** This player's name. */
    public String name;

    /**
     * Returns a blank name with just the user id.
     */
    public static PlayerName create (int userId)
    {
        PlayerName name = new PlayerName();
        name.userId = userId;
        return name;
    }
}
