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
        this(ctx, news, false);
        setStyleName("landing");
        addStyleName("page"); // we're a top-level page
    }

    public LandingPage (Context ctx, Value<News> news, boolean showNewbiesInstructions)
    {

        if (ctx.getMe().isGuest()) {
            addInstructions();
            add(Widgets.newHTML(ctx.getFacebookAddLink("Start playing now!"), "CTA", "machine"));

        } else if (showNewbiesInstructions && ctx.isNewbie()) {
            addInstructions();
            add(Widgets.newShim(10, 10));
            addBigFlip();

        } else if (ctx.isNewbie()) {
            addBigFlip();

        } else if (news.get() != null) {
            add(Widgets.newLabel("News: " + DateUtil.formatDateTime(news.get().reported),
                                 "Title", "machine"));
            add(Widgets.newHTML(formatNews(news.get().text), "Text"));
        }

        if (!ctx.getMe().isGuest()) {
            add(Widgets.newShim(5, 5));
            add(new MyFeedPanel(ctx));
        }

        if (ctx.isEditor()) {
            add(Widgets.newLabel("Build: " + Build.time(), "machine"));
        }
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

    protected void addBigFlip ()
    {
        add(Widgets.newLabel(_msgs.introBookmark(), "Title", "machine"));
        FluentTable intro = new FluentTable(5, 0);
        intro.add().setWidget(Widgets.newImage("images/bookmark_tip.png")).
            right().setText(_msgs.introBookmarkTip(), "Text");
        add(intro);
        add(Widgets.newShim(10, 10));
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
