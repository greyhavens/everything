//
// $Id$

package client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.PopupStack;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.EverythingCodes;
import com.threerings.everything.client.EverythingService;
import com.threerings.everything.client.EverythingServiceAsync;
import com.threerings.everything.data.Build;
import com.threerings.everything.data.News;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.SessionData;

import client.admin.DashboardPanel;
import client.editor.EditCatsPanel;
import client.editor.EditSeriesPanel;
import client.game.BrowsePanel;
import client.game.FriendsPanel;
import client.game.GridPanel;
import client.game.LandingPanel;
import client.game.ShopPanel;
import client.util.Args;
import client.util.CategoriesModel;
import client.util.Context;
import client.util.Errors;

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

        // validate our session which will trigger the rest of our initialization
        _everysvc.validateSession(
            Build.version(), getTimezoneOffset(), new AsyncCallback<SessionData>() {
            public void onSuccess (SessionData data) {
                if (data == null) {
                    setInfoContent("TODO: Redirect to splash/fbconnect page.");
                } else {
                    gotSessionData(data);
                }
            }
            public void onFailure (Throwable cause) {
                setContent(Widgets.newLabel(Errors.xlate(cause), "errorLabel"));
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
    public boolean isNewbie ()
    {
        return _data.gridsConsumed < NEWBIE_GRIDS;
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

    // from interface Context
    public Value<Integer> getCoins ()
    {
        return _coins;
    }

    // from interface Context
    public void displayPopup (PopupPanel popup)
    {
        _pstack.show(popup);
    }

    // from interface Context
    public CategoriesModel getCatsModel ()
    {
        return _catsmodel;
    }

    // from interface ValueChangeHandler<String>
    public void onValueChange (ValueChangeEvent<String> event)
    {
        Args args = new Args(event.getValue());

        switch (args.page) {
        default:
        case LANDING: setContent(new LandingPanel(this, _news)); break;
        case FLIP: setContent(new GridPanel(this)); break;
        case BROWSE: setContent(new BrowsePanel(this, args.get(0, getMe().userId))); break;
        case SHOP: setContent(new ShopPanel(this)); break;
        case FRIENDS: setContent(new FriendsPanel(this)); break;
        case EDIT_CATS:
            if (!(_content instanceof EditCatsPanel)) {
                setContent(new EditCatsPanel(this));
            }
            ((EditCatsPanel)_content).setArgs(args);
            break;
        case EDIT_SERIES: setContent(new EditSeriesPanel(this, args.get(0, 0))); break;
        case DASHBOARD: setContent(new DashboardPanel(this, _news)); break;
        }

        // if we have showing popups, clear them out
        _pstack.clear();
    }

    protected void gotSessionData (SessionData data)
    {
        _data = data;
        _coins = new Value<Integer>(data.coins);
        _news = new Value<News>(data.news);
        setContent(null);
        if (data.facebookKey != null) {
            initFacebook(data.facebookKey);
        }
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

    protected static native void initFacebook (String apiKey) /*-{
        $wnd.FB_Init(apiKey);
    }-*/;

    protected SessionData _data;
    protected Value<Integer> _coins;
    protected Value<News> _news;

    protected HeaderPanel _header;
    protected Widget _content;
    protected PopupStack _pstack = new PopupStack();

    protected CategoriesModel _catsmodel = new CategoriesModel(this);

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);

    protected static final String CLIENT_DIV = "client";
    protected static final int NEWBIE_GRIDS = 3;
}
