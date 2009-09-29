//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;

import client.util.Args;
import client.util.Context;
import client.util.Page;

/**
 * Displays a request to add our app.
 */
public class AddAppPanel extends FlowPanel
{
    public AddAppPanel (Context ctx, boolean asPage)
    {
        setStyleName("addApp");
        if (asPage) {
            addStyleName("page");
        }

        // add a bookmark hint once the player has added the app
        if (!ctx.getMe().isGuest()) {
            add(Widgets.newLabel(_msgs.introBookmark(), "Title", "machine"));
            FluentTable intro = new FluentTable(5, 0);
            intro.add().setWidget(Widgets.newImage("images/bookmark_tip.png")).
                right().setText(_msgs.introBookmarkTip(), "Text");
            add(intro);
            add(Widgets.newShim(10, 10));
        }

        add(Widgets.newLabel(_msgs.introTitle(), "Title", "machine"));
        add(Widgets.newLabel(_msgs.introIntro(), "Text"));
        FluentTable intro = new FluentTable(5, 0, "Steps");
        intro.add().setText(_msgs.introStepOne(), "machine").
            right().setText("\u2023", "Arrow").
            right().setText(_msgs.introStepTwo(), "machine").
            right().setText("\u2023", "Arrow").
            right().setText(_msgs.introStepThree(), "machine").
            right().setText("\u2023", "Arrow").
            right().setText(_msgs.introStepFour(), "machine");
        add(intro);

        if (ctx.getMe().isGuest()) {
            add(Widgets.newHTML("Click " + ctx.getFacebookAddLink("Add Everything") +
                                " to add the Everything app and start playing!", "machine"));
        } else {
            add(Widgets.newFlowPanel("Text", Widgets.newInlineLabel(_msgs.introReady() + " "),
                                     Args.createInlink(_msgs.introFlip(), Page.FLIP)));
        }
    }

    protected static final GameMessages _msgs = GWT.create(GameMessages.class);
}
