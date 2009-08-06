//
// $Id$

package client.util;

import java.util.MissingResourceException;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InfoPopup;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;

import client.util.Args;
import client.util.Page;

/**
 * Handles the translation of server errors.
 */
public class Errors
{
    /**
     * Displays an error popup, near the supplied widget if it is not null.
     */
    public static void showError (Throwable cause, Widget near)
    {
        InfoPopup popup;
        if (cause.getMessage().equals("e.nsf_for_flip")) {
            Hyperlink link = Args.createLink("Get Coins", Page.GET_COINS);
            link.addStyleName("machine");
            popup = new InfoPopup(Widgets.newRow(Widgets.newLabel(xlate(cause)),
                                                 Widgets.newShim(5, 5), link));
            link.addClickHandler(Popups.createHider(popup));
        } else {
            popup = new InfoPopup(xlate(cause));
        }
        if (near != null) {
            popup.showNear(near);
        } else {
            popup.showCentered();
        }
    }

    /**
     * Returns a friendly string that explains the supplied error.
     */
    public static String xlate (Throwable error)
    {
        return xlate(error.getMessage());
    }

    /**
     * Returns a friendly string that explains the supplied error code.
     */
    public static String xlate (String errcode)
    {
        if (errcode.startsWith("e.")) {
            try {
                return _dmsgs.xlate(errcode.substring(2));
            } catch (MissingResourceException mre) {
                // fall through and return the raw string
            }
        }
        return errcode;
    }

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
