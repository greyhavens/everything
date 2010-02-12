//
// $Id$

package client.game;

import java.util.List;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.StringUtil;
import com.threerings.gwt.util.Value;

import com.threerings.everything.data.Card;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.TrophyData;

import client.ui.LikeWidget;
import client.ui.ShadowPanel;
import client.ui.TrophyUI;
import client.util.Context;
import client.util.ImageUtil;

/**
 * Handles the display of a card.
 */
public abstract class CardView extends FlowPanel
{
    /**
     * Creates a view for the specified card.
     */
    public static Widget create (
        Context ctx, Card card, boolean showLike, String header, String status,
        List<TrophyData> trophies, Widget... buttons)
    {
        FluentTable box = new FluentTable(0, 0, "cardView", "handwriting");
        String bgimage = "images/info_card.png";
        if (header != null) {
            box.add().setHTML(header, "Header", "machine").setColSpan(2).alignBottom();
            box.addStyleName("cardViewTall");
            bgimage = "images/info_card_tall.png";
        }
        box.add().setWidget(new CardView.Left(ctx, card), "Left").
            right().setWidget(new CardView.Right(ctx, card, showLike, trophies), "Right");
        if (header != null) { // if we have a header, we always need a status
            box.add().setHTML(StringUtil.getOr(status, ""), "Status", "machine").setColSpan(2);
        }
        box.add().setWidget(Widgets.newRow(buttons), "Buttons").setColSpan(2).alignCenter();
        return new ShadowPanel(box, bgimage, "#EFE3C4", 2, 5, 6, 3);
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
        public Left (Context ctx, Card card)
        {
            FluentTable wrap = new FluentTable(0, 0, "Title");
            wrap.at(0, 0).setText(card.thing.name, getTitleSize(card.thing.name)).alignCenter();
            add(wrap);
            add(Widgets.newLabel((card.position+1) + " of " + card.things, "Position"));
            add(ImageUtil.getImageBox(card.thing.image));
            add(Widgets.newFlowPanel("Metrics", new RarityLabel("Rarity: ", card.thing.rarity),
                                     new CoinLabel(" - ", card.thing.rarity.value)));
        }
    }

    protected static class Right extends CardView
    {
        public Right (Context ctx, Card card, boolean showLike, List<TrophyData> trophies)
        {
            Widget cat = Widgets.newLabel(
                Category.getHierarchy(card.categories), "Categories",
                getCategoriesSize(card.categories));
            if (showLike) {
                LikeWidget like = new LikeWidget(ctx, card);
                FluentTable table = new FluentTable();
                table.add().setWidget(cat)
                    .right().setWidget(like).alignBottom().alignLeft();
                cat = table;
            }
            add(cat);
            FlowPanel info = Widgets.newFlowPanel(
                "Info", Widgets.newLabel(card.thing.descrip),
                Widgets.newLabel("Facts:", "FactsTitle"),
                Widgets.newHTML(formatFacts(card.thing.facts)),
                Widgets.newHTML("Source: " + sourceLink(card.thing.source), "Source"));
            info.addStyleName(card.categories[0].name);
            if (card.giver == null) {
                if (card.received != null) {
                    info.add(Widgets.newLabel("Flipped on: " + _dfmt.format(card.received),
                                              "When"));
                }
            } else if (card.giver.userId == Card.BIRTHDAY_GIVER_ID) {
                info.add(Widgets.newLabel("A birthday present from Everything", "Giver"));
                info.add(Widgets.newLabel("Received on: " + _dfmt.format(card.received), "When"));
            } else {
                info.add(Widgets.newLabel("A gift from " + card.giver, "Giver"));
                info.add(Widgets.newLabel("Received on: " + _dfmt.format(card.received), "When"));
            }
            if (trophies != null) {
                for (TrophyData trophy : trophies) {
                    info.add(TrophyUI.create(ctx, trophy, true));
                }
            }
            add(info);
        }
    }

    protected String getTitleSize (String name)
    {
        if (name.length() < 20) {
            return "NormalTitle";
        } else if (name.length() < 28) {
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
