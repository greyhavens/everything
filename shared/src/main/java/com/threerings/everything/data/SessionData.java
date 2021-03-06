//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Contains data needed by the client when we start a session.
 */
public class SessionData
    implements Serializable
{
    /** Our name and user id. */
    public PlayerName name;

    /** Whether or not this player is an editor. */
    public boolean isEditor;

    /** Whether or not this player is an admin. */
    public boolean isAdmin;

    /** Whether or not this player is a maintainer. */
    public boolean isMaintainer;

    /** The main URL to our app. */
    public String everythingURL;

    /** The URL of our backend app server. */
    public String backendURL;

    /** Our Facebook app ID. */
    public String facebookAppId;

    /** Our coin balance at the time we validated our session. */
    public int coins;

    /** This player's current powerups count. */
    public Map<Powerup, Integer> powerups;

    /** The categoryIds this player likes. */
    public List<Integer> likes;

    /** The categoryIds that this player dislikes. */
    public List<Integer> dislikes;

    /** Gifts awaiting this player, if any. */
    public List<ThingCard> gifts;

    /** Notices to deliver to the player. */
    public List<Notice> notices;

    /** The number of grids that this player has consumed. */
    public int gridsConsumed;

    /** When this player's grid expires. */
    public long gridExpires;

    /** The current game news, or null. */
    public News news;
}
