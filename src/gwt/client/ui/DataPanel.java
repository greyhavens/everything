//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;

import client.util.Context;
import client.util.PanelCallback;

/**
 * A base class for panels that load up some data and display it.
 */
public abstract class DataPanel<T> extends FlowPanel
{
    protected DataPanel (String styleName, Context ctx)
    {
        setStyleName(styleName);
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
                init(data);
            }
        };
    }

    protected Context _ctx;
}
