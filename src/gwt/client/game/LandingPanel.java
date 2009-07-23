//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.Value;

import com.threerings.everything.data.News;

import client.util.Context;

/**
 * The main interface displayed when the player arrives at the game.
 */
public class LandingPanel extends FlowPanel
{
    public LandingPanel (Context ctx, Value<News> news)
    {
        setStyleName("landing");
        addStyleName("page"); // we're a top-level page

        if (ctx.isNewbie()) {
            add(Widgets.newLabel("Welcome to Everything!", "Title"));
            FlowPanel intro = new FlowPanel();
            for (String text : INTRO_HTML) {
                intro.add(Widgets.newHTML(text, "Text"));
            }
            add(intro);
        }

        if (news.get() == null) {
            add(Widgets.newLabel("Latest News", "Title"));
            add(Widgets.newLabel("No gnus is good gnus.", "Text"));
        } else {
            add(Widgets.newLabel("News: " + DateUtil.formatDateTime(news.get().reported), "Title"));
            add(Widgets.newHTML(formatNews(news.get().text), "Text"));
        }

        add(Widgets.newLabel("Recent Happenings", "Title"));
        add(new FeedPanel(ctx));
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

    protected static final String[] INTRO_HTML = {
        "Everything is a collecting game. Every day you get a new grid of cards and you " +
        "flip them over to get new things to add to your collection. Some cards are rarer " +
        "than others, so good luck.",
        "You get three free flips every day, but you can buy more flips by spending coins " +
        "you get from cashing in cards you don't want. Rarer cards are worth more coins when " +
        "you cash them in. You can also give cards to your friends to help them complete their " +
        "collections.",
        "Enough jabber, click <b>Flip Cards</b> up above to get started!"
    };
}
