//
// $Id$

package client.game;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.client.EverythingServiceAsync;
import com.threerings.everything.data.PlayerName;

import client.ui.DataPanel;
import client.util.Args;
import client.util.Context;

/**
 * Displays credits. Whee!
 */
public class CreditsPage extends DataPanel<EverythingService.CreditsResult>
{
    public CreditsPage (Context ctx)
    {
        super(ctx, "page", "credits", "machine");
        _everysvc.getCredits(createCallback());
    }

    // from interface DataPanel
    protected void init (EverythingService.CreditsResult data)
    {
        add(Widgets.newLabel("The Everything Game", "Title"));
        int year = DateUtil.getYear(new Date());
        String copy = (year == 2009) ? "2009" : "2009-" + year;
        add(Widgets.newFlowPanel(
                "Subtitle", Widgets.newHTML("Copyright &copy; " + copy + " ", "inline"),
                new Anchor("http://www.threerings.net/", "Three Rings", "_blank")));

        FluentTable peeps = new FluentTable(10, 0, "Peeps");
        peeps.add().setWidgets(Widgets.newLabel("Design:"), personLink(data.design)).
            right().setWidgets(Widgets.newLabel("Art:"), personLink(data.art)).
            right().setWidgets(Widgets.newLabel("Code:"), personLink(data.code));

        FlowPanel editors = Widgets.newFlowPanel("Long");
        for (PlayerName editor : data.editors) {
            if (editors.getWidgetCount() > 0) {
                editors.add(Widgets.newInlineLabel(", "));
            }
            editors.add(personLink(editor));
        }
        peeps.add().setWidgets(Widgets.newLabel("Series Editors:"), editors).setColSpan(3);
        Widget thanks = Widgets.newLabel(SPECIAL_THANKS, "Long", "handwriting");
        peeps.add().setWidgets(Widgets.newLabel("Special Thanks:"), thanks).setColSpan(3);
        add(peeps);
    }

    protected Widget labeled (String label, Widget widget)
    {
        return Widgets.newFlowPanel(Widgets.newLabel(label), widget);
    }

    protected Widget personLink (PlayerName person)
    {
        if (person == null) {
            return Widgets.newInlineLabel("<unknown>", "handwriting");
        }
        Widget link = Args.createInlink(person);
        link.addStyleName("handwriting");
        return link;
    }

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
    protected static final String SPECIAL_THANKS =
        "To Daniel James for supplying the pink unicorns and " +
        "to the Dread Ringers for their tireless flipping.";
}
