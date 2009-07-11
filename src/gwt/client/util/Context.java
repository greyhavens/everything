//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.everything.data.PlayerName;

import com.threerings.gwt.util.Value;

/**
 * Provides access to client services.
 */
public interface Context
{
    /** Configures the main client display. */
    public void setContent (Widget widget);

    /** Returns this player's name. */
    public PlayerName getMe ();

    /** Returns whether this player has editor privileges. */
    public boolean isEditor ();

    /** Returns whether this player has admin privileges. */
    public boolean isAdmin ();

    /** Returns the dynamic value that contains our current coin balance. */
    public Value<Integer> getCoins ();

    /** Displays a popup, hiding any existing popup (which will be restored when this popup is
     * cleared). */
    public void displayPopup (PopupPanel popup);
}
