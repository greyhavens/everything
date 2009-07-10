//
// $Id$

package client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.client.EverythingServiceAsync;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.SessionData;

import client.admin.AdminPanel;
import client.game.MainPanel;
import client.util.Context;

/**
 * The entry point for the Everything client.
 */
public class EverythingClient
    implements EntryPoint, Context, ValueChangeHandler<String>
{
    // from interface EntryPoint
    public void onModuleLoad ()
    {
        setInfoContent("Initializing...");
        History.addValueChangeHandler(this);
        _everysvc.validateSession(getTimezoneOffset(), new AsyncCallback<SessionData>() {
            public void onSuccess (SessionData data) {
                if (data == null) {
                    setInfoContent("TODO: Redirect to splash/fbconnect page.");
                } else {
                    _data = data;
                    History.fireCurrentHistoryState();
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

    // from interface ValueChangeHandler<String>
    public void onValueChange (ValueChangeEvent<String> event)
    {
        String token = event.getValue();
        if (token.startsWith("admin")) {
            if (!(_content instanceof AdminPanel)) {
                setContent(new AdminPanel(this));
            }
            String[] toks = token.split("-", 2);
            ((AdminPanel)_content).setToken(toks.length == 2 ? toks[1] : "");

        } else {
            if (!(_content instanceof MainPanel)) {
                setContent(new MainPanel(this));
            }
            ((MainPanel)_content).setToken(token);
        }
    }

    protected Widget createMainContent ()
    {
        String token = History.getToken();
        if (token.startsWith("admin")) {
            return new AdminPanel(this);
        } else {
            return new MainPanel(this);
        }
    }

    protected void setInfoContent (String message)
    {
        setContent(Widgets.newLabel(message, "infoLabel"));
    }

    protected static native int getTimezoneOffset () /*-{
        return new Date().getTimezoneOffset();
    }-*/;

    protected SessionData _data;
    protected Widget _content;

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);

    protected static final String CLIENT_DIV = "client";
}
