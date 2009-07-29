//
// $Id$

package client.game;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.SmartTable;
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
        int year = DateUtil.getYear(new Date()) + 1900;
        String copy = (year == 2009) ? "2009" : "2009-" + year;
        add(Widgets.newFlowPanel(
                "Subtitle", Widgets.newHTML("Copyright &copy; " + copy + " ", "inline"),
                new Anchor("http://www.threerings.net/", "Three Rings", "_blank")));

        SmartTable peeps = new SmartTable("Peeps", 10, 0);
        peeps.setWidget(0, 0, labeled("Design:", personLink(data.design)));
        peeps.setWidget(0, 1, labeled("Art:", personLink(data.art)));
        peeps.setWidget(0, 2, labeled("Code:", personLink(data.code)));
        FlowPanel editors = new FlowPanel();
        for (PlayerName editor : data.editors) {
            if (editors.getWidgetCount() > 0) {
                editors.add(Widgets.newInlineLabel(", "));
            }
            editors.add(personLink(editor));
        }
        peeps.setWidget(1, 0, labeled("Series Editors:", editors), 3);
        peeps.setWidget(2, 0, labeled("Special Thanks:",
                                      Widgets.newLabel(SPECIAL_THANKS, "handwriting")), 3);
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
