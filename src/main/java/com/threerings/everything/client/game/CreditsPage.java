//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.game;

import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;

import com.threerings.everything.rpc.EverythingService;
import com.threerings.everything.rpc.EverythingServiceAsync;
import com.threerings.everything.data.PlayerName;

import com.threerings.everything.client.ui.DataPanel;
import com.threerings.everything.client.util.Args;
import com.threerings.everything.client.util.Context;

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
        String copy = "2009-" + DateUtil.getYear(new Date());
        add(Widgets.newFlowPanel(
                "Subtitle", Widgets.newHTML("Copyright &copy; " + copy + " ", "inline"),
                new Anchor("http://www.threerings.net/", "Three Rings", "_blank")));

        FluentTable peeps = new FluentTable(10, 0, "Peeps");
        peeps.add().setWidgets(Widgets.newLabel("Design:"), personLink(data.design)).
            right().setWidgets(Widgets.newLabel("Art:"), personLink(data.art)).
            right().setWidgets(
                Widgets.newLabel("Code:"), addPeeps(Widgets.newFlowPanel(), data.code));

        peeps.add().setWidgets(Widgets.newLabel("Series Editors:"),
            addPeeps(Widgets.newFlowPanel("Long"), data.editors)).setColSpan(3);
        Widget thanks = Widgets.newLabel(SPECIAL_THANKS, "Long", "handwriting");
        peeps.add().setWidgets(Widgets.newLabel("Special Thanks:"), thanks).setColSpan(3);
        add(peeps);
    }

    protected Panel addPeeps (Panel panel, List<PlayerName> peeps)
    {
        int added = 0;
        for (PlayerName peep : peeps) {
            if (added > 0) {
                panel.add(Widgets.newInlineLabel(", "));
            }
            panel.add(personLink(peep));
            added++;
        }
        return panel;
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
