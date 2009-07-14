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

    /** This player's surname. */
    public String surname;

    /**
     * Formats this name for viewing.
     */
    public String toString ()
    {
        return name + " " + surname;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof PlayerName) && userId == ((PlayerName)other).userId;
    }

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
