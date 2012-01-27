//
// $Id$

package com.threerings.everything.client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;

/**
 * Displays our terms of service.
 */
public class TermsPage extends FlowPanel
{
    public TermsPage ()
    {
        setStyleName("page");
        add(Widgets.newHTML(_msgs.termsOfService()));
    }

    protected GameMessages _msgs = GWT.create(GameMessages.class);
}
