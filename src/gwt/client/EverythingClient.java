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

import client.admin.EditThingsPanel;
import client.game.BrowsePanel;
import client.game.GridPanel;
import client.game.LandingPanel;
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
                    gotSessionData(data);
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
    public boolean isEditor ()
    {
        return _data.isEditor || _data.isAdmin;
    }

    // from interface Context
    public boolean isAdmin ()
    {
        return _data.isAdmin;
    }

    // from interface ValueChangeHandler<String>
    public void onValueChange (ValueChangeEvent<String> event)
    {
        Page page;
        try {
            page = Enum.valueOf(Page.class, event.getValue());
        } catch (Exception e) {
            page = Page.LANDING;
        }

        switch (page) {
        default:
        case LANDING: setContent(new LandingPanel(this)); break;
        case FLIP: setContent(new GridPanel(this)); break;
        case BROWSE: setContent(new BrowsePanel(this, getMe().userId)); break;
        case EDIT_THINGS: setContent(new EditThingsPanel(this)); break;
        }
    }

    protected void gotSessionData (SessionData data)
    {
        _data = data;
        setContent(null);
        RootPanel.get(CLIENT_DIV).add(_header = new HeaderPanel(this));
        History.fireCurrentHistoryState();
    }

    protected void setInfoContent (String message)
    {
        setContent(Widgets.newLabel(message, "infoLabel"));
    }

    protected static native int getTimezoneOffset () /*-{
        return new Date().getTimezoneOffset();
    }-*/;

    protected SessionData _data;
    protected HeaderPanel _header;
    protected Widget _content;

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);

    protected static final String CLIENT_DIV = "client";
}
