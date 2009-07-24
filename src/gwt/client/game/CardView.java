//
// $Id$

package client.game;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.StringUtil;

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
    public static Widget create (Card card, String header, String status, Button... buttons)
    {
        SmartTable box = new SmartTable("cardView", 0, 0);
        int row = 0;
        if (header != null) {
            box.setHTML(row++, 0, header, 3, "Header", "machine");
            box.addStyleName("cardViewTall");
        }
        box.setWidget(row, 0, new CardView.Left(card), 1, "Left");
        box.setWidget(row++, 2, new CardView.Right(card), 1, "Right");
        if (header != null) { // if we have a header, we always need a status
            box.setHTML(row++, 0, StringUtil.getOr(status, ""), 3, "Status", "machine");
        }
        box.setWidget(row, 0, Widgets.newRow(buttons), 3, "Buttons");
        box.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_CENTER);
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

    protected static String sourceLink (String source)
    {
        return "<a target=\"_blank\" href=\"" + source + "\">" + nameSource(source) + "</a>";
    }

    protected static String formatFacts (String facts)
    {
        StringBuilder buf = new StringBuilder();
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
            SmartTable wrap = new SmartTable("Title", 0, 0);
            wrap.setText(0, 0, card.thing.name, 1, getTitleSize(card.thing.name), "handwriting");
            wrap.getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
            add(wrap);
            add(Widgets.newLabel((card.position+1) + " of " + card.things, "Position"));
            add(ImageUtil.getImageBox(card.thing.image));
            add(Widgets.newFlowPanel("Metrics", new RarityLabel("Rarity: ", card.thing.rarity),
                                     new CoinLabel(" - ", card.thing.rarity.value)));
        }
    }

    protected static class Right extends CardView
    {
        public Right (Card card)
        {
            add(Widgets.newLabel(Category.getHierarchy(card.categories), "Categories",
                                 getCategoriesSize(card.categories), "handwriting"));
            add(Widgets.newFlowPanel(
                    "Info", Widgets.newLabel(card.thing.descrip, "handwriting"),
                    Widgets.newLabel("Facts:", "FactsTitle", "handwriting"),
                    Widgets.newHTML(formatFacts(card.thing.facts), "handwriting"),
                    Widgets.newHTML("Source: " + sourceLink(card.thing.source),
                                    "Source", "handwriting")));
            if (card.giver != null) {
                add(Widgets.newLabel("A gift from " + card.giver, "Giver", "handwriting"));
            }
            add(Widgets.newLabel("Received on: " + _dfmt.format(card.created),
                                 "When", "handwriting"));
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
        if (length < 26) {
            return "NormalCategories";
        } else {
            return "LongCategories";
        }
    }

    protected static final DateTimeFormat _dfmt = DateTimeFormat.getLongDateFormat();
}
