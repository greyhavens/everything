//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.util;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.everything.rpc.Kontagent;
import com.threerings.everything.data.PlayerName;

import com.threerings.gwt.util.Value;

/**
 * Provides access to client services.
 */
public interface Context
{
    /** The number of grids a user must have seen before we consider them fairly experienced. */
    public static final int EXPERIENCED_GRIDS = 20;

    /** Configures the main client display. */
    void setContent (Widget widget);

    /** Returns this player's name. */
    PlayerName getMe ();

    /** Returns the URL that will navigate directly to Everything. */
    String getEverythingURL (String vector, Page page, Object... args);

    /** Returns the URL that will navigate directly to Everything. */
    String getEverythingURL (Kontagent type, String tracking, Page page, Object... args);

    /** Returns the URL to the page that will add our app. */
    String getFacebookAddURL (Kontagent type, String tracking, Page page, Object... args);

    /** Returns HTML for an anchor tag that will add our app. */
    String getFacebookAddLink (String text);

    /** Returns HTML for an anchor tag that will add our app. */
    String getFacebookAddLink (String text, Page page, Object... args);

    /** Returns the URL to the specified billing page. */
    String billingURL (String path);

    /** Returns true if this player is still pretty new. */
    boolean isNewbie ();

    /** Returns the number of grids this user has consumed. */
    int getGridsConsumed ();

    /** Returns whether this player has editor privileges. */
    boolean isEditor ();

    /** Returns whether this player has admin privileges. */
    boolean isAdmin ();

    /** Returns whether this player has maintainer privileges. */
    boolean isMaintainer ();

    /** Returns the dynamic value that contains our current coin balance. */
    Value<Integer> getCoins ();

    /** Returns the time at which our current grid expires. */
    Value<Long> getGridExpiry ();

    /** Get (or create) the 'like' setting for the specified category.
     * The Boolean may be null if the user has no setting yet for this category. */
    Value<Boolean> getLike (int categoryId);

    /** Displays a popup, hiding any existing popup (which will be restored when this popup is
     * cleared). The popup will be vertically centered on the supplied optional widget. */
    void displayPopup (PopupPanel popup, Widget onCenter);

    /** A value that reports whether or not a popup is showing. */
    Value<Boolean> popupShowing ();

    /**
     * Returns the categories model.
     */
    CategoriesModel getCatsModel ();

    /**
     * Returns the powerups model.
     */
    PowerupsModel getPupsModel ();
}
