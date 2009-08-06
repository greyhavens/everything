//
// $Id$

package client.util;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Extends the stock click callback and provides proper error translation.
 */
public abstract class ClickCallback<T> extends com.threerings.gwt.util.ClickCallback<T>
{
    /**
     * See {@link com.threerings.gwt.util.ClickCallback(HasClickHandlers}}.
     */
    public ClickCallback (HasClickHandlers trigger)
    {
        super(trigger);
    }

    /**
     * See {@link com.threerings.gwt.util.ClickCallback(HasClickHandlers,TextBox}}.
     */
    public ClickCallback (HasClickHandlers trigger, TextBox onEnter)
    {
        super(trigger, onEnter);
    }

    @Override // from ClickCallback<T>
    protected void showError (Throwable cause, Widget near)
    {
        Errors.showError(cause, near);
    }
}
