//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.AbstractPopupCallback;

/**
 * A callback that displays errors via a popup.
 */
public abstract class PopupCallback<T> extends AbstractPopupCallback<T>
{
    public PopupCallback ()
    {
    }

    public PopupCallback (Widget errorNear)
    {
        super(errorNear);
    }

    @Override // from AbstractPopupCallback<T>
    protected String formatError (Throwable cause)
    {
        return cause.getMessage(); // TODO
    }
}
