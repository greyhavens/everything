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
    void setContent (Widget widget);

    /** Returns this player's name. */
    PlayerName getMe ();

    /** Returns true if this player is still pretty new. */
    boolean isNewbie ();

    /** Returns whether this player has editor privileges. */
    boolean isEditor ();

    /** Returns whether this player has admin privileges. */
    boolean isAdmin ();

    /** Returns the dynamic value that contains our current coin balance. */
    Value<Integer> getCoins ();

    /** Returns the time at which our current grid expires. */
    long getGridExpiry ();

    /** Displays a popup, hiding any existing popup (which will be restored when this popup is
     * cleared). */
    void displayPopup (PopupPanel popup);

    /**
     * Returns the categories model.
     */
    CategoriesModel getCatsModel ();

    /**
     * Returns the powerups model.
     */
    PowerupsModel getPupsModel ();
}
