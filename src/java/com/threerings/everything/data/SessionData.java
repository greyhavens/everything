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

    /** Whether or not this player is a maintainer. */
    public boolean isMaintainer;

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

    /** Whether or not we're running in the candidate. */
    public boolean candidate;

    /** The main URL to our app. */
    public String everythingURL;

    /** A URL to request from Kontagent once we're loaded. */
    public String kontagentHello;

    /** The Kontagent UUID associated with this session (for guests). */
    public String kontagentToken;
}
