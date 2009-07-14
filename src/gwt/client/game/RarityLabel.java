//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.Label;

import com.threerings.everything.data.Rarity;

/**
 * Displays a rarity along with a tooltip with more detailed info.
 */
public class RarityLabel extends Label
{
    public RarityLabel (Rarity rarity)
    {
        setStyleName("Rarity");
        addStyleName("inline");
        if (rarity == null) {
            setText("?");
        } else {
            setText(rarity.toString());
            setTitle(rarity.description());
        }
    }
}
