//
// $Id$

package client;

import com.google.gwt.user.client.ui.Hyperlink;

import com.threerings.gwt.ui.SmartTable;

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
        setWidget(0, col++, new Hyperlink("Your Collection", ""+Page.BROWSE));
        setWidget(0, col++, new Hyperlink("Flip Cards", ""+Page.FLIP));
        if (ctx.isEditor()) {
            setWidget(0, col++, new Hyperlink("Add Things", ""+Page.EDIT_THINGS));
        }
    }
}
