//
// $Id$

package client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import client.game.MainPanel;

/**
 * The entry point for the Everything client.
 */
public class EverythingClient implements EntryPoint
{
    // from interface EntryPoint
    public void onModuleLoad ()
    {
        setContent(new MainPanel());
    }

    protected void setContent (Widget content)
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

    protected Widget _content;

    protected static final String CLIENT_DIV = "client";
}
