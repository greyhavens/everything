//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.Panel;

import com.threerings.gwt.util.AbstractPanelCallback;

/**
 * A callback that displays errors with a label stuffed into a panel.
 */
public abstract class PanelCallback<T> extends AbstractPanelCallback<T>
{
    public PanelCallback (Panel panel)
    {
        super(panel);
    }

    @Override // from AbstractPanelCallback<T>
    protected String formatError (Throwable cause)
    {
        return cause.getMessage(); // TODO
    }
}
