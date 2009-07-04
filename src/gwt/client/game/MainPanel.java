//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.client.EverythingServiceAsync;
import com.threerings.everything.data.SessionData;

/**
 * Contains the main game UI.
 */
public class MainPanel extends FlowPanel
{
    public MainPanel ()
    {
        _everysvc.validateSession(new AsyncCallback<SessionData>() {
            public void onSuccess (SessionData data) {
                if (data == null) {
                    add(new Label("You need to add the app! (TODO)."));
                } else {
                    add(new Label("Hello " + data.name));
                }
            }
            public void onFailure (Throwable cause) {
                add(new Label("Error: " + cause.getMessage()));
            }
        });
    }

    protected static final EverythingServiceAsync _everysvc = (EverythingServiceAsync)
        GWT.create(EverythingService.class);
}
