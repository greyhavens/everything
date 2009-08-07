//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.SmartTable;
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

        add(Widgets.newLabel(_msgs.introTitle(), "Title", "machine"));
        add(Widgets.newLabel(_msgs.introIntro(), "Text"));
        int col = 0;
        SmartTable intro = new SmartTable("Steps", 5, 0);
        intro.setText(0, col++, _msgs.introStepOne(), 1, "machine");
        intro.setText(0, col++, "\u2023", 1, "Arrow");
        intro.setText(0, col++, _msgs.introStepTwo(), 1, "machine");
        intro.setText(0, col++, "\u2023", 1, "Arrow");
        intro.setText(0, col++, _msgs.introStepThree(), 1, "machine");
        intro.setText(0, col++, "\u2023", 1, "Arrow");
        intro.setText(0, col++, _msgs.introStepFour(), 1, "machine");
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
