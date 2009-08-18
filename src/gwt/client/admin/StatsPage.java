//
// $Id$

package client.admin;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.NumberTextBox;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.AdminService;
import com.threerings.everything.client.AdminServiceAsync;
import com.threerings.everything.data.Category;

import client.ui.DataPanel;
import client.util.Args;
import client.util.ClickCallback;
import client.util.Context;
import client.util.Page;

/**
 * Displays an admin dashboard.
 */
public class StatsPage extends DataPanel<AdminService.StatsResult>
{
    public StatsPage (Context ctx)
    {
        super(ctx, "page", "stats");
        _adminsvc.getStats(createCallback());
    }

    protected void init (final AdminService.StatsResult data)
    {
        FluentTable contents = new FluentTable(5, 0);
        add(contents);

        // set up our thing database stats
        FlowPanel stats = new FlowPanel();
        stats.add(Widgets.newLabel("Thing Stats", "machine"));
        stats.add(Widgets.newLabel("Total things: " + data.stats.totalThings));
        stats.add(Widgets.newLabel("Total categories: " + data.stats.totalCategories));
        stats.add(Widgets.newLabel("Total players: " + data.stats.totalPlayers));
        stats.add(Widgets.newLabel("Total cards: " + data.stats.totalCards));

        final NumberTextBox flips = NumberTextBox.newIntBox(2, 2);
        final Button grant = new Button("Grant");
        stats.add(Widgets.newRow(Widgets.newLabel("Grant free flips:"), flips, grant));
        new ClickCallback<Void>(grant, flips) {
            protected boolean callService () {
                int togrant = flips.getNumber().intValue();
                if (togrant <= 0) {
                    return false;
                }
                _adminsvc.grantFreeFlips(0, togrant, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                Popups.infoNear("Free flips granted to everyone!", grant);
                return true;
            }
        }.setConfirmText("Are you sure you want to grant free flips to every player in the " +
                         "entire game?");

        contents.at(0, 0).setWidget(stats).alignTop();

        // display our pending categories
        FlowPanel cats = new FlowPanel();
        for (Category cat : data.pendingCategories) {
            if (cats.getWidgetCount() > 0) {
                cats.add(Widgets.newInlineLabel(", "));
            }
            Widget link = Args.createInlink(cat.name, Page.EDIT_SERIES, cat.categoryId);
            if (cat.state == Category.State.PENDING_REVIEW) {
                link.addStyleName("Pending");
            }
            cats.add(link);
        }
        contents.at(0, 1).setWidgets(Widgets.newLabel("Pending Categories", "machine"), cats).
            alignTop().setRowSpan(2);
    }

    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);
}