//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.client.EverythingServiceAsync;

/**
 * Contains the main game UI.
 */
public class MainPanel extends FlowPanel
{
    public MainPanel ()
    {
        // TODO
    }

    protected static final EverythingServiceAsync _everysvc = (EverythingServiceAsync)
        GWT.create(EverythingService.class);
}
