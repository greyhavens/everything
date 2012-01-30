//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

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

    /** This player's Facebook id. */
    public long facebookId;

    /** This player's name. */
    public String name;

    /** This player's surname. */
    public String surname;

    /**
     * Returns true if we're a guest.
     */
    public boolean isGuest ()
    {
        return userId == 0;
    }

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

    @Override // from Object
    public int hashCode ()
    {
        return userId;
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

    /**
     * Creates a name for a guest player.
     */
    public static PlayerName createGuest ()
    {
        PlayerName name = create(0);
        name.name = "Guest";
        return name;
    }
}
