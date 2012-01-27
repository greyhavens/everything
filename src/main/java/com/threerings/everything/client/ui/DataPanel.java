//
// $Id$

package com.threerings.everything.client.ui;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Console;

import com.threerings.everything.client.util.Context;
import com.threerings.everything.client.util.PanelCallback;

/**
 * A base class for panels that load up some data and display it.
 */
public abstract class DataPanel<T> extends FlowPanel
{
    protected DataPanel (Context ctx, String... styleNames)
    {
        for (String styleName : styleNames) {
            addStyleName(styleName);
        }
        add(Widgets.newLabel("Loading...", "infoLabel"));
        _ctx = ctx;
    }

    /**
     * This is called by the callback when the data has been loaded.
     */
    protected abstract void init (T data);

    /**
     * Creates a callback that should be passed to the service method that obtains the data.
     */
    protected PanelCallback<T> createCallback ()
    {
        return new PanelCallback<T>(this) {
            public void onSuccess (T data) {
                clear();
                try {
                    init(data);
                } catch (Exception e) {
                    Console.log("DataPanel.init failed", e);
                    if (getWidgetCount() == 0) {
                        add(Widgets.newLabel("Uh oh Spaghetti-Os! Something broke.", "infoLabel"));
                    }
                }
            }
        };
    }

    protected Context _ctx;
}
