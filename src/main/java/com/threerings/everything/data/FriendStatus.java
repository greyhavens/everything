//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains info on a friend that is shown on the friends page.
 */
public class FriendStatus
    implements IsSerializable
{
    /** This friend's name. */
    public PlayerName name;

    /** The time at which this friend was last online. */
    public Date lastSession;
}
