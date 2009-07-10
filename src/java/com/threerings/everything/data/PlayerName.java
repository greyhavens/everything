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

    /** Used when unserializing. */
    public PlayerName ()
    {
    }

    /** Creates an initialized player name. */
    public PlayerName (int userId, String name)
    {
        this.userId = userId;
        this.name = name;
    }
}
