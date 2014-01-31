//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.Value;

import com.threerings.everything.data.Build;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.News;
import com.threerings.everything.rpc.GameService;
import com.threerings.everything.rpc.GameServiceAsync;

import com.threerings.everything.client.ui.XFBML;
import com.threerings.everything.client.util.Args;
import com.threerings.everything.client.util.Context;
import com.threerings.everything.client.util.Page;
import com.threerings.everything.client.util.PanelCallback;

/**
 * The main interface displayed when the player arrives at the game.
 */
public class LandingPage extends FlowPanel
{
    protected abstract static class Mode {
        protected static class ShowNews extends Mode {
            public Value<News> news;
        }
        protected static class Attractor extends Mode {
            public int attractorId;
            public int friendId;
        }
        protected static class ShowCard extends Mode {
            public CardIdent ident;
        }
    }

    public static LandingPage news (Context ctx, Value<News> news) {
        Mode.ShowNews mode = new Mode.ShowNews();
        mode.news = news;
        return new LandingPage(ctx, mode);
    }

    public static LandingPage attractor (Context ctx, int attractorId, int friendId) {
        Mode.Attractor mode = new Mode.Attractor();
        mode.attractorId = attractorId;
        mode.friendId = friendId;
        return new LandingPage(ctx, mode);
    }

    public static LandingPage card (Context ctx, int ownerId, int thingId, long received) {
        Mode.ShowCard mode = new Mode.ShowCard();
        mode.ident = new CardIdent(ownerId, thingId, received);
        return new LandingPage(ctx, mode);
    }

    private LandingPage (final Context ctx, Mode mode)
    {
        boolean isGuest = ctx.getMe().isGuest();
        setStyleName("landing");
        addStyleName("page"); // we're a top-level page

        if (mode instanceof Mode.Attractor) {
            Mode.Attractor amode = (Mode.Attractor)mode;
            if (isGuest) {
                // This is a non-normal landing, they should have added the app before
                // trying to get the attractor. Cope.
                add(Widgets.newHTML(
                        ctx.getFacebookAddLink("Play everything and get the card you want",
                                               Page.ATTRACTOR, amode.attractorId, amode.friendId)));
                return;
            }
            add(new AttractorPanel(ctx, amode.attractorId, amode.friendId));
            return;

        } else if (mode instanceof Mode.ShowCard) {
            addInstructions(false);
            add(Widgets.newShim(5, 5));
            final FlowPanel box = Widgets.newFlowPanel("CardBox");
            box.add(Widgets.newLabel("Loading..."));
            add(box);
            _gamesvc.getCard(((Mode.ShowCard)mode).ident, new PanelCallback<Card>(box) {
                public void onSuccess (Card card) {
                    box.clear();
                    box.add(CardView.create(ctx, card, false, null, null));
                }
            });
            add(Widgets.newShim(5, 5));
            addMobileLinks(ctx, true);
            return;
        }

        if (isGuest) {
            addInstructions(true);
            add(Widgets.newHTML(ctx.getFacebookAddLink("Start playing now!"), "CTA", "machine"));
            add(Widgets.newShim(5, 5));
            addMobileLinks(ctx, false);
            return;
        }

        if (ctx.isNewbie()) {
            // show extra help for a new players
            addInstructions(true);
            add(Widgets.newShim(10, 10));

        } else if (mode instanceof Mode.ShowNews) {
            Mode.ShowNews nmode = (Mode.ShowNews)mode;
            if (newsNotStale(nmode.news.get())) {
                String time = DateUtil.formatDateTime(nmode.news.get().reported);
                add(Widgets.newLabel(_msgs.newsTitle(time), "Title", "machine"));
                add(Widgets.newHTML(formatNews(nmode.news.get().text), "Text"));
                add(Widgets.newShim(10, 10));
            }
        }

        // at this point, nobody is a guest
        Widget link = Args.createLink(_msgs.introFlip(), Page.FLIP);
        link.addStyleName("BigFlip");
        link.addStyleName("machine");
        add(link);
        add(Widgets.newShim(10, 10));

        // add links to the mobile version
        addMobileLinks(ctx, false);
        add(Widgets.newShim(5, 5));

        add(new MyFeedPanel(ctx));

        if (ctx.isEditor()) {
            add(Widgets.newLabel("Build: " + Build.time(), "machine"));
        }
    }

    @Override
    protected void onLoad ()
    {
        super.onLoad();
        // Strangely, this doesn't seem to be necessary. Maybe we're getting handled
        // by the header?
        XFBML.parse(this);
    }

    protected void addMobileLinks (Context ctx, boolean showFacebook)
    {
        FluentTable mobile = new FluentTable(5, 0, "Mobile");
        int ii = 0;
        if (showFacebook) {
            addLink(mobile, ii++, "Play Everything in your web browser on Facebook!",
                    "images/facebook.png", ctx.getFacebookAddURL("show_card", Page.CARD));
        }
        addLink(mobile, ii++, "Play Everything on your Android phone or tablet!",
                "images/googleplay.png", PLAYSTORE_URL);
        addLink(mobile, ii++, "Play Everything on your iPhone or iPad!", "images/appstore.svg",
                APPSTORE_URL);
        addLink(mobile, ii++, "Meet other players in our Google+ Community!",
                "//ssl.gstatic.com/images/icons/gplus-32.png", GOOGPLUS_URL);
        add(mobile);
    }

    protected void addLink (FluentTable table, int col, String text, String imageURL, String url) {
        table.at(0, col).setText(text, "Text");
        table.at(1, col).setWidget(new Anchor("<img src='" + imageURL + "'/>", true, url, "_top"));
    }

    protected void addInstructions (boolean showHow)
    {
        add(Widgets.newLabel(_msgs.introTitle(), "Title", "machine"));
        add(Widgets.newLabel(_msgs.introIntro(), "Text"));
        if (showHow) {
            add(Widgets.newLabel(_msgs.introHow(), "Text"));
            FluentTable intro = new FluentTable(5, 0, "Steps");
            intro.add().setText(_msgs.introStepOne(), "machine").
                right().setText("\u2023", "Arrow").
                right().setText(_msgs.introStepTwo(), "machine").
                right().setText("\u2023", "Arrow").
                right().setText(_msgs.introStepThree(), "machine").
                right().setText("\u2023", "Arrow").
                right().setText(_msgs.introStepFour(), "machine");
            add(intro);
        }
    }

    protected static String formatNews (String text)
    {
        StringBuilder buf = new StringBuilder();
        boolean inList = false;
        for (String line : text.split("\n")) {
            if (line.startsWith("* ")) {
                if (!inList) {
                    buf.append("<ul>\n");
                    inList = true;
                }
                buf.append("<li>").append(line.substring(2)).append("</li>\n");
            } else {
                if (inList) {
                    buf.append("</ul>\n");
                    inList = false;
                }
                buf.append(line).append("\n");
            }
        }
        if (inList) {
            buf.append("</ul>");
        }
        return buf.toString();
    }

    protected static boolean newsNotStale (News news)
    {
        return (news != null) &&
            ((System.currentTimeMillis() - news.reported.getTime()) < STALE_NEWS_MILLIS);
    }

    protected static final GameMessages _msgs = GWT.create(GameMessages.class);
    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);

    protected static final String PLAYSTORE_URL =
        "https://play.google.com/store/apps/details?id=com.threerings.everything";
    protected static final String APPSTORE_URL = "https://itunes.apple.com/app/id670486442";
    protected static final String GOOGPLUS_URL =
        "https://plus.google.com/u/0/communities/115671345917573066171?prsrc=3";

    // news is considered stale after it's been up for a week
    protected static final long STALE_NEWS_MILLIS = 7*24*60*60*1000L;
}
