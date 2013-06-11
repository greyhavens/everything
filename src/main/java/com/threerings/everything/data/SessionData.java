//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.util.List;
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

    /** The categoryIds this player likes. */
    public List<Integer> likes;

    /** The categoryIds that this player dislikes. */
    public List<Integer> dislikes;

    /** The number of grids that this player has consumed. */
    public int gridsConsumed;

    /** When this player's grid expires. */
    public long gridExpires;

    /** The current game news, or null. */
    public News news;

    /** The main URL to our app. */
    public String everythingURL;

    /** The URL of our backend app server. */
    public String backendURL;

    /** The URL root for all billing pages. */
    public String billingRootURL;

    /** Our Facebook app ID. */
    public String facebookAppId;

    /** A URL to request from Kontagent once we're loaded. */
    public String kontagentHello;
}
