//
// $Id$

package client.editor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;

import client.util.Context;

/**
 * A brief intro to editing for non-editors.
 */
public class EditIntroPage extends FlowPanel
{
    public EditIntroPage (Context ctx)
    {
        addStyleName("page");
        add(Widgets.newHTML(_msgs.nonEditor(), "Text"));
    }

    protected static final EditorMessages _msgs = GWT.create(EditorMessages.class);
}
