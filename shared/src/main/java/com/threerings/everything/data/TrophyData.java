//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.data;

import java.io.Serializable;

/**
 * Contains display data for trophies owned by a user.
 */
public class TrophyData
    implements Serializable
{
    /** The trophy identifier. */
    public String trophyId;

    /** The name of the trophy. */
    public String name;

    /** The description of the trophy. */
    public String description;

    /** Suitable for deserialization. */
    public TrophyData () {}

    /**
     * Constructor.
     */
    public TrophyData (String trophyId, String name, String description)
    {
        this.trophyId = trophyId;
        this.name = name;
        this.description = description;
    }
}
