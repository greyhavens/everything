//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.Value;

import com.threerings.everything.data.Build;
import com.threerings.everything.data.News;

import client.ui.XFBML;
import client.util.Args;
import client.util.Context;
import client.util.Page;

/**
 * The main interface displayed when the player arrives at the game.
 */
public class LandingPage extends FlowPanel
{
    public LandingPage (Context ctx, Value<News> news)
    {
        this(ctx, news, 0, 0);
    }

    public LandingPage (Context ctx, int attractorId, int friendId)
    {
        this(ctx, null, attractorId, friendId);
    }

    private LandingPage (Context ctx, Value<News> news, int attractorId, int friendId)
    {
        setStyleName("landing");
        addStyleName("page"); // we're a top-level page

        boolean isGuest = ctx.getMe().isGuest();
        if (attractorId != 0) {
            if (isGuest) {
                // This is a non-normal landing, they should have added the app before
                // trying to get the attractor. Cope.
                add(Widgets.newHTML(
                    ctx.getFacebookAddLink("Play everything and get the card you want",
                        Page.ATTRACTOR, attractorId, friendId)));
                return;
            }
            add(new AttractorPanel(ctx, attractorId, friendId));

        } else if (isGuest) {
            addInstructions();
            add(Widgets.newHTML(ctx.getFacebookAddLink("Start playing now!"), "CTA", "machine"));
            return;
        }

        // TODO: use FQL to determine if they've bookmarked or not and provide additional prompting
        add(XFBML.newInlineTag("bookmark"));

        if (attractorId != 0 && ctx.isNewbie()) {
            // show extra help for a new player that arrived via an attractor
            addInstructions();
            add(Widgets.newShim(10, 10));
            //addBookmarkTip();

        } else if (ctx.isNewbie()) {
            // don't confuse newbies with too many options
            //addBookmarkTip();

        } else if (news != null && news.get() != null) {
            add(Widgets.newLabel("News: " + DateUtil.formatDateTime(news.get().reported),
                                 "Title", "machine"));
            add(Widgets.newHTML(formatNews(news.get().text), "Text"));
            add(Widgets.newShim(10, 10));
        }

        // at this point, nobody is a guest
        addBigFlip();
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

    protected void addInstructions ()
    {
        add(Widgets.newLabel(_msgs.introTitle(), "Title", "machine"));
        add(Widgets.newLabel(_msgs.introIntro(), "Text"));
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

    protected void addBookmarkTip ()
    {
        add(Widgets.newLabel(_msgs.introBookmark(), "Title", "machine"));
        FluentTable intro = new FluentTable(5, 0);
        intro.add().setWidget(Widgets.newImage("images/bookmark_tip.png")).
            right().setText(_msgs.introBookmarkTip(), "Text");
        add(intro);
        add(Widgets.newShim(10, 10));
    }

    protected void addBigFlip ()
    {
        Widget link = Args.createLink(_msgs.introFlip(), Page.FLIP);
        link.addStyleName("BigFlip");
        link.addStyleName("machine");
        add(link);
        add(Widgets.newShim(10, 10));
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

    protected static final GameMessages _msgs = GWT.create(GameMessages.class);
}
