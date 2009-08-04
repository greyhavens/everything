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
    public GetCoinsPage (Context ctx)
    {
        addStyleName("getCoins");
        add(new Frame(Build.billingURL()));
    }
}
