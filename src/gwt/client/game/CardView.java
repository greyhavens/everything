//
// $Id$

package client.game;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.data.Card;
import com.threerings.everything.data.Category;

import client.util.ImageUtil;

/**
 * Handles the display of a card.
 */
public abstract class CardView extends FlowPanel
{
    /**
     * Creates a view for the specified card.
     */
    public static Widget create (Card card)
    {
        SmartTable box = new SmartTable("cardView", 0, 0);
        box.setWidget(0, 0, new CardView.Left(card), 1, "Left");
        box.setWidget(0, 1, Widgets.newShim(5, 5));
        box.setWidget(0, 2, new CardView.Right(card), 1, "Right");
        return box;
    }

    protected static String nameSource (String source)
    {
        if (source.indexOf("wikipedia.org") != -1) {
            return "Wikipedia";
        }

        int ssidx = source.indexOf("//");
        int eidx = source.indexOf("/", ssidx+2);
        if (ssidx == -1) {
            return source;
        } else if (eidx == -1) {
            return source.substring(ssidx+2);
        } else {
            return source.substring(ssidx+2, eidx);
        }
    }

    protected static String formatFacts (String facts)
    {
        StringBuffer buf = new StringBuffer();
        buf.append("<ul>");
        for (String bit : facts.split("\n")) {
            buf.append("<li>").append(bit).append("</li>");
        }
        return buf.append("</ul>").toString();
    }

    protected static class Left extends CardView
    {
        public Left (Card card)
        {
            add(Widgets.newLabel(card.thing.name, "Title"));
            StringBuilder buf = new StringBuilder();
            for (Category cat : card.categories) {
                if (buf.length() > 0) {
                    buf.append(" &#8594; "); // right arrow
                }
                buf.append(cat.name);
            }
            add(Widgets.newHTML(buf.toString(), "Categories"));
            add(ImageUtil.getImageBox(card.thing.image));
            add(Widgets.newFlowPanel("Metrics", new RarityLabel("Rarity: ", card.thing.rarity),
                                     new CoinLabel(" - ", card.thing.rarity.value)));
        }
    }

    protected static class Right extends CardView
    {
        public Right (Card card)
        {
            // add(Widgets.newLabel(card.thing.name, "Title"));
            add(Widgets.newLabel("N of M", "Position"));
            add(Widgets.newLabel(card.thing.descrip, "Descrip"));
            add(Widgets.newLabel("Facts:", "FactsTitle"));
            add(Widgets.newHTML(formatFacts(card.thing.facts), "Facts"));
            add(Widgets.newLabel("Received on: " + _dfmt.format(card.created), "When"));
            if (card.giver != null) {
                add(Widgets.newLabel("A gift from " + card.giver.name, "Giver"));
            }
            add(Widgets.newHTML("Source: <a target=\"_blank\" href=\"" + card.thing.source + "\">" +
                                nameSource(card.thing.source) + "</a>", "Source"));
        }
    }

    protected static final DateTimeFormat _dfmt = DateTimeFormat.getLongDateFormat();
}
