//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.util;

import com.google.gwt.user.client.ui.Panel;

/**
 * A callback that displays errors with a label stuffed into a panel.
 */
public abstract class PanelCallback<T> extends com.threerings.gwt.util.PanelCallback<T>
{
    public PanelCallback (Panel panel)
    {
        super(panel);
    }

    @Override // from AbstractPanelCallback<T>
    protected String formatError (Throwable cause)
    {
        return Errors.xlate(cause);
    }
}
