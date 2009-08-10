//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.Value;

import com.threerings.everything.data.Build;
import com.threerings.everything.data.News;

import client.util.Context;

/**
 * The main interface displayed when the player arrives at the game.
 */
public class LandingPage extends FlowPanel
{
    public LandingPage (Context ctx, Value<News> news)
    {
        setStyleName("landing");
        addStyleName("page"); // we're a top-level page

        if (ctx.getMe().isGuest() || ctx.isNewbie()) {
            add(new AddAppPanel(ctx, false));
        } else if (news.get() == null) {
            add(Widgets.newLabel("Latest News", "Title"));
            add(Widgets.newLabel("No gnus is good gnus.", "Text"));
        } else {
            add(Widgets.newLabel("News: " + DateUtil.formatDateTime(news.get().reported), "Title"));
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
