//
// $Id$

package client.game;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.data.Card;
import com.threerings.everything.data.Category;

import client.ui.FlashBuilder;
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
        FlashBuilder fb = new FlashBuilder("card", "info_card");
        fb.addOr("title", card.thing.name, "?");
        fb.add("category", Category.getHierarchy(card.categories));
        fb.addIf("image", card.thing.image);
        fb.add("rarity", "Rarity: " + card.thing.rarity + " - Σ" + card.thing.rarity.value);
        fb.add("series", (card.position+1) + " of " + card.things);
        fb.add("descrip", card.thing.descrip);
        fb.add("facts", card.thing.facts);
        fb.add("from", (card.giver == null) ? "" : "A gift from " + card.giver);
        fb.add("date", "Received on: " + _dfmt.format(card.created));
        fb.add("source", card.thing.source);
        return fb.build(560, 380, false);
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
            add(Widgets.newLabel(card.thing.name, "Title", getTitleSize(card.thing.name)));
            add(Widgets.newLabel(Category.getHierarchy(card.categories), "Categories",
                                 getCategoriesSize(card.categories)));
            add(ImageUtil.getImageBox(card.thing.image));
            add(Widgets.newFlowPanel("Metrics", new RarityLabel("Rarity: ", card.thing.rarity),
                                     new CoinLabel(" - ", card.thing.rarity.value)));
        }
    }

    protected static class Right extends CardView
    {
        public Right (Card card)
        {
            add(Widgets.newLabel((card.position+1) + " of " + card.things, "Position"));
            add(Widgets.newLabel(card.thing.descrip, "Descrip"));
            add(Widgets.newLabel("Facts:", "FactsTitle"));
            add(Widgets.newHTML(formatFacts(card.thing.facts), "Facts"));
            add(Widgets.newHTML("Source: <a target=\"_blank\" href=\"" + card.thing.source + "\">" +
                                nameSource(card.thing.source) + "</a>", "Source"));
            if (card.giver != null) {
                add(Widgets.newLabel("A gift from " + card.giver, "Giver"));
            }
            add(Widgets.newLabel("Received on: " + _dfmt.format(card.created), "When"));
        }
    }

    protected String getTitleSize (String name)
    {
        if (name.length() < 16) {
            return "NormalTitle";
        } else if (name.length() < 24) {
            return "LongTitle";
        } else {
            return "ReallyLongTitle";
        }
    }

    protected String getCategoriesSize (Category[] categories)
    {
        int length = 0;
        for (Category cat : categories) {
            length += cat.name.length();
        }
        if (length < 28) {
            return "NormalCategories";
        } else {
            return "LongCategories";
        }
    }

    protected static final DateTimeFormat _dfmt = DateTimeFormat.getLongDateFormat();
}
