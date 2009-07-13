//
// $Id$

package client.util;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;

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
    protected void takeAction ()
    {
        takeAction(false);
    }

    /**
     * If a callback wishes to require confirmation it can override this method and return a
     * message that will be displayed to confirm the action before it is taken.
     */
    protected String getConfirmMessage ()
    {
        return null;
    }

    /**
     * Returns the choices given to the user when confirming the callback. The default choices on
     * the confirm dialog are "No", "Yes".
     */
    protected String[] getConfirmChoices ()
    {
        return new String[] { "No", "Yes" };
    }

    /**
     * Override this method and return true if you wish your confirm message to be interpreted as
     * HTML. Be careful!
     */
    protected boolean confirmMessageIsHTML ()
    {
        return false;
    }

    protected void takeAction (boolean confirmed)
    {
        // if we have no confirmation message or are already confirmed, do the deed
        String confmsg = getConfirmMessage();
        if (confirmed || confmsg == null) {
            if (callService()) {
                setEnabled(false);
            }
            return;
        }

        // otherwise display a confirmation panel
        final PopupPanel confirm = new PopupPanel();
        confirm.setStyleName("confirm");
        SmartTable contents = new SmartTable(5, 0);
        if (confirmMessageIsHTML()) {
            contents.setHTML(0, 0, confmsg, 2, null);
        } else {
            contents.setText(0, 0, confmsg, 2, null);
        }
        String[] choices = getConfirmChoices();
        contents.setWidget(1, 0, new Button(choices[0], new ClickHandler() {
            public void onClick (ClickEvent event) {
                confirm.hide(); // abort!
            }
        }));
        contents.setWidget(1, 1, new Button(choices[1], new ClickHandler() {
            public void onClick (ClickEvent event) {
                confirm.hide();
                takeAction(true);
            }
        }));
        contents.getFlexCellFormatter().setHorizontalAlignment(1, 1, HasAlignment.ALIGN_RIGHT);
        confirm.setWidget(contents);
        confirm.center();
    }
}
