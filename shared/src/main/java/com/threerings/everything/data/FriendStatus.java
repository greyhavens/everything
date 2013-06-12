//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.io.Serializable;
import java.util.Date;

/**
 * Contains info on a friend that is shown on the friends page.
 */
public class FriendStatus
    implements Serializable
{
    /** This friend's name. */
    public PlayerName name;

    /** The time at which this friend was last online. */
    public Date lastSession;
}
