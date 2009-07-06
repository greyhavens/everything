//
// $Id$

package client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.client.EverythingServiceAsync;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.SessionData;

import client.game.MainPanel;
import client.util.Context;

/**
 * The entry point for the Everything client.
 */
public class EverythingClient
    implements EntryPoint, Context
{
    // from interface EntryPoint
    public void onModuleLoad ()
    {
        setInfoContent("Initializing...");
        _everysvc.validateSession(new AsyncCallback<SessionData>() {
            public void onSuccess (SessionData data) {
                if (data == null) {
                    setInfoContent("TODO: Redirect to splash/fbconnect page.");
                } else {
                    _data = data;
                    setContent(new MainPanel(EverythingClient.this));
                }
            }
            public void onFailure (Throwable cause) {
                setInfoContent("Oh noez: " + cause.getMessage()); // TODO
            }
        });
    }

    // from interface Context
    public void setContent (Widget content)
    {
        if (_content != null) {
            RootPanel.get(CLIENT_DIV).remove(_content);
            _content = null;
        }
        if (content != null) {
            _content = content;
            RootPanel.get(CLIENT_DIV).add(_content);
        }
    }

    // from interface Context
    public PlayerName getMe ()
    {
        return _data.name;
    }

    // from interface Context
    public boolean isAdmin ()
    {
        return _data.isAdmin;
    }

    protected void setInfoContent (String message)
    {
        setContent(Widgets.newLabel(message, "infoLabel"));
    }

    protected SessionData _data;
    protected Widget _content;

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);

    protected static final String CLIENT_DIV = "client";
}
