//
// $Id$

package com.threerings.everything.data;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains data needed by the client when we start a session.
 */
public class SessionData
    implements IsSerializable
{
    /** Our name and user id. */
    public PlayerName name;

    /** Whether or not this player is an editor. */
    public boolean isEditor;

    /** Whether or not this player is an admin. */
    public boolean isAdmin;

    /** Our coin balance at the time we validated our session. */
    public int coins;

    /** This player's current powerups count. */
    public Map<Powerup, Integer> powerups;

    /** The number of grids that this player has consumed. */
    public int gridsConsumed;

    /** When this player's grid expires. */
    public long gridExpires;

    /** The current game news, or null. */
    public News news;

    /** Our Facebook API key. */
    public String facebookKey;
}
