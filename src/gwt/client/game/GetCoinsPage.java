//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;

import com.threerings.everything.data.Build;

import client.util.Context;

/**
 * Displays the billing website wrapped in an iframe;
 */
public class GetCoinsPage extends FlowPanel
{
    public GetCoinsPage (Context ctx, String action)
    {
        addStyleName("getCoins");
        String path;
        if (action.equals("admin")) {
            path = Build.billingURL("admin/");
        } else {
            path = Build.billingURL("select_type.wm");
        }
        add(new Frame(path));
    }
}
