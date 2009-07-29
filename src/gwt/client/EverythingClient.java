//
// $Id$

package client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
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
import client.game.AddAppPanel;
import client.game.BrowsePanel;
import client.game.FriendsPanel;
import client.game.GridPanel;
import client.game.LandingPanel;
import client.game.ShopPanel;
import client.ui.XFBML;
import client.util.Args;
import client.util.CategoriesModel;
import client.util.Context;
import client.util.Errors;
import client.util.PowerupsModel;

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
                gotSessionData(data);
            }
            public void onFailure (Throwable cause) {
                setContent(Widgets.newLabel(Errors.xlate(cause), "errorLabel"));
            }
        });
    }

    // from interface Context
    public void setContent (Widget content)
    {
        if (_wrapper != null) {
            RootPanel.get(CLIENT_DIV).remove(_wrapper);
            _wrapper = null;
            _content = null;
        }
        if (content != null) {
            _wrapper = Widgets.newFlowPanel(Widgets.newSimplePanel("content", _content = content),
                                            Widgets.newImage("images/page_cap.png", "endcap"));
            RootPanel.get(CLIENT_DIV).add(_wrapper);
        }
    }

    // from interface Context
    public PlayerName getMe ()
    {
        return _data.name;
    }

    // from interface Context
    public String getFacebookAddURL ()
    {
        return "http://www.facebook.com/login.php?api_key=" + _data.facebookKey + "&canvas=1&v=1.0";
    }

    // from interface Context
    public String getFacebookAddLink (String text)
    {
        StringBuilder buf = new StringBuilder();
        buf.append("<a target=\"_top\" href=\"").append(getFacebookAddURL()).append("\">");
        return buf.append(text).append("</a>").toString();
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
    public Value<Long> getGridExpiry ()
    {
        return _gridExpires;
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

    // from interface Context
    public PowerupsModel getPupsModel ()
    {
        return _pupsmodel;
    }

    // from interface ValueChangeHandler<String>
    public void onValueChange (ValueChangeEvent<String> event)
    {
        Args args = new Args(event.getValue());

        // if we have showing popups, clear them out
        _pstack.clear();

        // these are OK to view as a guest
        switch (args.page) {
        case LANDING:
            setContent(new LandingPanel(this, _news));
            return;
        case BROWSE:
            if (args.get(0, getMe().userId) != 0) {
                if (!(_content instanceof BrowsePanel)) {
                    setContent(new BrowsePanel(this));
                }
                ((BrowsePanel)_content).setArgs(args.get(0, getMe().userId), args.get(1, 0));
                return;
            }
            break;
        }

        // otherwise guests see the "add this app to play" button
        if (getMe().isGuest()) {
            setContent(new AddAppPanel(this, true));
            return;
        }

        // these are OK as a regular player
        switch (args.page) {
        case FLIP:
            setContent(new GridPanel(this));
            return;
        case SHOP:
            setContent(new ShopPanel(this));
            return;
        case FRIENDS:
            setContent(new FriendsPanel(this));
            return;
        }

        // these are OK for editors
        if (isEditor()) {
            switch (args.page) {
            case EDIT_CATS:
                if (!(_content instanceof EditCatsPanel)) {
                    setContent(new EditCatsPanel(this));
                }
                ((EditCatsPanel)_content).setArgs(args);
                return;
            case EDIT_SERIES:
                setContent(new EditSeriesPanel(this, args.get(0, 0)));
                return;
            }
        }

        // these are OK for admins
        if (isAdmin()) {
            switch (args.page) {
            case DASHBOARD:
                setContent(new DashboardPanel(this, _news));
                return;
            }
        }

        // otherwise default to displaying the landing panel
        setContent(new LandingPanel(this, _news));
    }

    protected void gotSessionData (SessionData data)
    {
        _data = data;
        _coins = new Value<Integer>(data.coins);
        _gridExpires = new Value<Long>(data.gridExpires);
        _news = new Value<News>(data.news);
        _pupsmodel = new PowerupsModel(data.powerups);
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
        $wnd.FB_RequireFeatures(["XFBML"], function () {
            $wnd.FB.init(apiKey, "xd_receiver.html");
        });

        // start up our iframe resizer once FB_Init is called by GWT
        if ($wnd != $wnd.top) {
            $wnd.FB.Bootstrap.ensureInit(function () {
                $wnd.FB.CanvasClient.startTimerToSizeToContent();
            });
        }
    }-*/;

    protected SessionData _data;
    protected Value<Integer> _coins;
    protected Value<Long> _gridExpires;
    protected Value<News> _news;

    protected HeaderPanel _header;
    protected Widget _content, _wrapper;
    protected PopupStack _pstack = new PopupStack();

    protected CategoriesModel _catsmodel = new CategoriesModel(this);
    protected PowerupsModel _pupsmodel;

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);

    protected static final String CLIENT_DIV = "client";
    protected static final int NEWBIE_GRIDS = 3;
}
