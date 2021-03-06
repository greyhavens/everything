//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
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

import com.threerings.everything.rpc.EverythingService;
import com.threerings.everything.rpc.EverythingServiceAsync;
import com.threerings.everything.data.Build;
import com.threerings.everything.data.News;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.SessionData;

import com.threerings.everything.client.admin.EditNewsPage;
import com.threerings.everything.client.admin.StatsPage;
import com.threerings.everything.client.editor.EditCatsPage;
import com.threerings.everything.client.editor.EditFAQPage;
import com.threerings.everything.client.editor.EditIntroPage;
import com.threerings.everything.client.editor.EditPendingPage;
import com.threerings.everything.client.editor.EditSeriesPage;
import com.threerings.everything.client.editor.InDevPage;
import com.threerings.everything.client.game.BrowsePage;
import com.threerings.everything.client.game.BuyCoinsPage;
import com.threerings.everything.client.game.CreditsPage;
import com.threerings.everything.client.game.FlipPage;
import com.threerings.everything.client.game.FriendsPage;
import com.threerings.everything.client.game.LandingPage;
import com.threerings.everything.client.game.ShopPage;
import com.threerings.everything.client.util.Args;
import com.threerings.everything.client.util.CategoriesModel;
import com.threerings.everything.client.util.Context;
import com.threerings.everything.client.util.Errors;
import com.threerings.everything.client.util.Page;
import com.threerings.everything.client.util.PowerupsModel;

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
        _everysvc.validateSession(Build.version(), getTimezoneOffset(),
                                  new AsyncCallback<SessionData>() {
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
            _content = content;
            _wrapper = Widgets.newFlowPanel(
                Widgets.newSimplePanel("content", content),
                Widgets.newImage("images/page_cap.png", "endcap"),
                Widgets.newHTML("Everything's <a href=\"/privacy.html\">Privacy Policy</a>"));
            RootPanel.get(CLIENT_DIV).add(_wrapper);
        }
    }

    // from interface Context
    public PlayerName getMe ()
    {
        return _data.name;
    }

    // from interface Context
    public String getEverythingURL (String vector, Page page, Object... args)
    {
        return _data.everythingURL + "?token=" + Args.createLinkToken(page, args) +
            "&vec=" + vector;
    }

    // from interface Context
    public String getFacebookAddURL (String vector, Page page, Object... args)
    {
        return "https://www.facebook.com/dialog/oauth?client_id=" + _data.facebookAppId +
            "&redirect_uri=" + URL.encodeQueryString(getEverythingURL(vector, page, args));
    }

    // from interface Context
    public String getFacebookAddLink (String text)
    {
        return getFacebookAddLink(text, Page.LANDING);
    }

    // from interface Context
    public String getFacebookAddLink (String text, Page page, Object... args)
    {
        String url = getFacebookAddURL("app_added", page, args);
        StringBuilder buf = new StringBuilder();
        buf.append("<a target=\"_top\" href=\"").append(url).append("\">");
        return buf.append(text).append("</a>").toString();
    }

    // from interface Context
    public String backendURL ()
    {
        return _data.backendURL;
    }

    // from interface Context
    public boolean isNewbie ()
    {
        return _data.gridsConsumed < NEWBIE_GRIDS;
    }

    // from interface Context
    public int getGridsConsumed ()
    {
        return _data.gridsConsumed;
    }

    // from interface Context
    public boolean isEditor ()
    {
        return _data.isEditor || isAdmin();
    }

    // from interface Context
    public boolean isAdmin ()
    {
        return _data.isAdmin || isMaintainer();
    }

    // from interface Context
    public boolean isMaintainer ()
    {
        return _data.isMaintainer;
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
    public Value<Boolean> getLike (int categoryId)
    {
        Value<Boolean> like = _likes.get(categoryId);
        if (like == null) {
            like = Value.create(null);
            _likes.put(categoryId, like);
        }
        return like;
    }

    // from interface Context
    public void displayPopup (PopupPanel popup, Widget onCenter)
    {
        _pstack.show(popup, onCenter);
    }

    // from interface Context
    public Value<Boolean> popupShowing ()
    {
        return _pstack.popupShowing;
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
        case ATTRACTOR:
            setContent(LandingPage.attractor(this, args.get(0, 0), args.get(1, 0)));
            return;
        case LANDING:
            setContent(LandingPage.news(this, _news));
            return;
        case CARD:
            setContent(LandingPage.card(this, args.get(0, 0), args.get(1, 0), args.get(2, 0L)));
            return;
        case CREDITS:
            setContent(new CreditsPage(this));
            return;
        case BROWSE:
            if (args.get(0, getMe().userId) != 0) {
                if (!(_content instanceof BrowsePage)) {
                    setContent(new BrowsePage(this));
                }
                ((BrowsePage)_content).setArgs(args.get(0, getMe().userId),
                                               args.get(1, 0), args.get(1, ""));
                return;
            }
            break;
        default:
            break; // keep going
        }

        // otherwise guests see the "add this app to play" button
        if (getMe().isGuest()) {
            setContent(LandingPage.news(this, _news));
            return;
        }

        // these are OK as a regular player
        switch (args.page) {
        case FLIP:
            setContent(new FlipPage(this));
            return;
        case SHOP:
            setContent(new ShopPage(this));
            return;
        case GET_COINS:
            setContent(new BuyCoinsPage(this, args.get(0, "")));
            return;
        case FRIENDS:
            if (!(_content instanceof FriendsPage)) {
                setContent(new FriendsPage(this));
            }
            ((FriendsPage)_content).setMode(args.get(0, ""));
            return;
        case EDIT_INTRO:
            setContent(new EditIntroPage(this));
            return;
        default:
            break; // keep going
        }

        // these are OK for editors
        if (isEditor()) {
            switch (args.page) {
            case EDIT_CATS:
                if (!(_content instanceof EditCatsPage)) {
                    setContent(new EditCatsPage(this));
                }
                ((EditCatsPage)_content).setArgs(args);
                return;
            case EDIT_PENDING:
                setContent(new EditPendingPage(this));
                return;
            case EDIT_INDEV:
                if (!(_content instanceof InDevPage)) {
                    setContent(new InDevPage(this));
                }
                ((InDevPage)_content).setMode(args.get(0, ""));
                return;
            case EDIT_FAQ:
                setContent(new EditFAQPage(this));
                return;
            case EDIT_SERIES:
                setContent(new EditSeriesPage(this, args.get(0, 0)));
                return;
            default:
                break; // keep going
            }
        }

        // these are OK for admins
        if (isAdmin()) {
            switch (args.page) {
            case ADMIN_STATS:
                setContent(new StatsPage(this));
                return;
            case ADMIN_NEWS:
                setContent(new EditNewsPage(this, _news));
                return;
            default:
                break; // keep going
            }
        }

        // otherwise default to displaying the landing panel
        setContent(LandingPage.news(this, _news));
    }

    protected void gotSessionData (SessionData data)
    {
        _data = data;
        _coins = new Value<Integer>(data.coins);
        _gridExpires = new Value<Long>(data.gridExpires);
        _news = new Value<News>(data.news);
        _pupsmodel = new PowerupsModel(data.powerups);
        _likes = new HashMap<Integer, Value<Boolean>>();
        for (Integer like : data.likes) {
            _likes.put(like, Value.create(true));
        }
        for (Integer dislike : data.dislikes) {
            _likes.put(dislike, Value.create(false));
        }

        setContent(null);
        initFacebook(data.facebookAppId, GWT.getHostPageBaseURL() + "channel.cache.html");
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

    protected static native void initFacebook (String appId, String channelUrl) /*-{
        $wnd.fbAsyncInit = function () {
            $wnd.FB.init({ appId: appId, channelUrl: channelUrl, status: true, cookie: true });
            // start up our iframe resizer if we're running in an iframe
            if ($wnd != $wnd.top) $wnd.FB.Canvas.setAutoGrow();
            $wnd.FB.XFBML.parse(); // grind through once on init
        };

        (function(d, debug){
           var js, id = 'facebook-jssdk', ref = d.getElementsByTagName('script')[0];
           if (d.getElementById(id)) {return;}
           js = d.createElement('script'); js.id = id; js.async = true;
           js.src = "//connect.facebook.net/en_US/all" + (debug ? "/debug" : "") + ".js";
           ref.parentNode.insertBefore(js, ref);
        }($wnd.document, false));
    }-*/;

    protected SessionData _data;
    protected Value<Integer> _coins;
    protected Value<Long> _gridExpires;
    protected Value<News> _news;
    protected Map<Integer, Value<Boolean>> _likes;

    protected HeaderPanel _header;
    protected Widget _content, _wrapper;
    protected PopupStack _pstack = new PopupStack();

    protected CategoriesModel _catsmodel = new CategoriesModel(this);
    protected PowerupsModel _pupsmodel;

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);

    protected static final String CLIENT_DIV = "client";
    protected static final int NEWBIE_GRIDS = 3;
}
