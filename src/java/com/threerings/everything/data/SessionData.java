//
// $Id$

package com.threerings.everything.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains data needed by the client when we start a session.
 */
public class SessionData
    implements IsSerializable
{
    /** Our name and user id. */
    public PlayerName name;

    /** Whether or not this player is an admin. */
    public boolean isAdmin;
}
