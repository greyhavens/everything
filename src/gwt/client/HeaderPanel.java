//
// $Id$

package client;

import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Hyperlink;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.everything.data.Build;

import client.util.Context;

/**
 * Displays a simple header and navigation.
 */
public class HeaderPanel extends SmartTable
{
    public HeaderPanel (Context ctx)
    {
        super("header", 5, 0);

        int col = 0;
        setText(0, col++, "Hello: " + ctx.getMe().name);
        setWidget(0, col++, new Hyperlink("News", ""+Page.LANDING));
        setWidget(0, col++, new Hyperlink("Your Collection", ""+Page.BROWSE));
        setWidget(0, col++, new Hyperlink("Flip Cards", ""+Page.FLIP));
        if (ctx.isEditor()) {
            setWidget(0, col++, new Hyperlink("Add Things", ""+Page.EDIT_THINGS));
        }
        if (ctx.isAdmin()) {
            setWidget(0, col++, new Hyperlink("Dashboard", ""+Page.DASHBOARD));
        }

        // finally display the game build on the right
        getFlexCellFormatter().setHorizontalAlignment(0, col, HasAlignment.ALIGN_RIGHT);
        getFlexCellFormatter().setWidth(0, col, "100%");
        setText(0, col++, "Build: " + Build.VERSION);
    }
}
