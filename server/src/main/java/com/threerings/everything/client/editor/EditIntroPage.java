//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.editor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.util.Context;

/**
 * A brief intro to editing for non-editors.
 */
public class EditIntroPage extends FlowPanel
{
    public EditIntroPage (Context ctx)
    {
        addStyleName("page");
        addStyleName("editIntro");
        // add(Widgets.newHTML(_msgs.nonEditor(), "Text"));
        add(Widgets.newHTML(_msgs.notNow(), "Text"));
    }

    protected static final EditorMessages _msgs = GWT.create(EditorMessages.class);
}
