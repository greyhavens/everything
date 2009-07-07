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

/**
 * Handles the display of the front and back of a card.
 */
public abstract class CardView extends FlowPanel
{
    public static class Front extends CardView
    {
        public Front (Card card)
        {
            addStyleName("Front");

            add(Widgets.newLabel(card.thing.name, "Title"));

            StringBuilder buf = new StringBuilder();
            for (Category cat : card.categories) {
                if (buf.length() > 0) {
                    buf.append(" &#8594; "); // right arrow
                }
                buf.append(cat.name);
            }
            add(newHTML(buf.toString(), "Categories"));

            add(boxImage(card.thing.image));

            add(newHTML(card.thing.rarity + " - &curren;" + card.thing.rarity.value, "Rarity"));
        }
    }

    public static class Back extends CardView
    {
        public Back (Card card)
        {
            addStyleName("Back");

            add(Widgets.newLabel(card.thing.name, "Title"));
            add(Widgets.newLabel("N of M", "Position"));

            add(Widgets.newLabel(card.thing.descrip, "Descrip"));

            add(Widgets.newLabel("Facts:", "FactsTitle"));

            add(newHTML(formatFacts(card.thing.facts), "Facts"));

            add(Widgets.newLabel("Received on: " + _dfmt.format(card.created), "When"));
            if (card.giver != null) {
                add(Widgets.newLabel("A gift from " + card.giver.name, "Giver"));
            }

            add(newHTML("Source: <a target=\"_blank\" href=\"" + card.thing.source + "\">" +
                        nameSource(card.thing.source) + "</a>", "Source"));
        }
    }

    protected CardView ()
    {
        setStyleName("cardView");
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

    protected static HTML newHTML (String text, String styleName)
    {
        HTML html = new HTML(text);
        html.setStyleName(styleName);
        return html;
    }

    protected static Widget boxImage (String path)
    {
        SmartTable table = new SmartTable("Image", 0, 0);
        table.setWidget(0, 0, new Image(path.length() == 0 ? "images/placeholder.png" : path));
        table.getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
        table.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_MIDDLE);
        return table;
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

    protected DateTimeFormat _dfmt = DateTimeFormat.getLongDateFormat();
}
