//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.everything.data.PlayerName;

/**
 * Provides access to client services.
 */
public interface Context
{
    /** Configures the main client display. */
    public void setContent (Widget widget);

    /** Returns this player's name. */
    public PlayerName getMe ();

    /** Returns whether this player is an admin. */
    public boolean isAdmin ();
}
