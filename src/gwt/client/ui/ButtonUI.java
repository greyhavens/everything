//
// $Id$

package client.ui;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.PushButton;

/**
 * Provides button related UI bits.
 */
public class ButtonUI
{
    /**
     * Creates a fancily styled button with a big happy border. The text should not exceed seven
     * characters.
     */
    public static PushButton newButton (String text)
    {
        return newButton(text, null);
    }

    /**
     * Creates a fancily styled button with a big happy border. The text should not exceed seven
     * characters.
     */
    public static PushButton newButton (String text, ClickHandler onClick)
    {
        PushButton button = new PushButton(text);
        if (text.length() > 5) {
            button.addStyleName("longButton");
        }
        if (onClick != null) {
            button.addClickHandler(onClick);
        }
        return button;
    }

    /**
     * Creates a button with a smallish border. The text should not exceed five characters.
     */
    public static PushButton newSmallButton (String text)
    {
        return newSmallButton(text, null);
    }

    /**
     * Creates a button with a smallish border. The text should not exceed five characters.
     */
    public static PushButton newSmallButton (String text, ClickHandler onClick)
    {
        PushButton button = new PushButton(text);
        button.setStyleName((text.length() <= 4) ? "smallButton" : "smallWideButton");
        if (onClick != null) {
            button.addClickHandler(onClick);
        }
        return button;
    }
}
