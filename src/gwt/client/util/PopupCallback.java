//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.Widget;

/**
 * A callback that displays errors via a popup.
 */
public abstract class PopupCallback<T> extends com.threerings.gwt.util.PopupCallback<T>
{
    public PopupCallback ()
    {
    }

    public PopupCallback (Widget errorNear)
    {
        super(errorNear);
    }

    @Override // from PopupCallback<T>
    public void onFailure (Throwable cause)
    {
        Errors.showError(cause, _errorNear);
    }
}
