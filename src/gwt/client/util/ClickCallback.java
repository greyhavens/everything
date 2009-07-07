//
// $Id$

package client.util;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Popups;

/**
 * Specializes the stock ClickCallback by handling errors appropriately.
 */
public abstract class ClickCallback<T> extends com.threerings.gwt.util.ClickCallback<T>
{
    public ClickCallback (HasClickHandlers trigger)
    {
        super(trigger);
    }

    public ClickCallback (HasClickHandlers trigger, TextBox onEnter)
    {
        super(trigger, onEnter);
    }

    @Override // from ClickCallback<T>
    protected void reportFailure (Throwable cause)
    {
        Widget near = _onEnter;
        if (near == null && _trigger instanceof Widget) {
            near = (Widget)_trigger;
        }
        if (_onEnter != null) {
            _onEnter.setFocus(true);
        }
        if (near == null) {
            Popups.error(cause.getMessage());
        } else {
            Popups.errorNear(cause.getMessage(), near);
        }
    }
}
