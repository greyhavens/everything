//
// $Id$

package client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * The entry point for the Everything client.
 */
public class EverythingClient implements EntryPoint
{
    // from interface EntryPoint
    public void onModuleLoad ()
    {
        setContent(new Label("TODO"));
    }

    protected void setContent (Panel content)
    {
        if (_content != null) {
            RootPanel.get(CLIENT_DIV).remove(_content);
            _content = null;
        }
        if (content != null) {
            _content = content;
            RootPanel.get(CLIENT_DIV).add(_content);
        }
    }

    protected Panel _content;

    protected static final String CLIENT_DIV = "client";
}
