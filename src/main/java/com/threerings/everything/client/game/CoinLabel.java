//
// $Id$

package com.threerings.everything.client.game;

import com.threerings.gwt.ui.ValueHTML;
import com.threerings.gwt.util.Value;

/**
 * Displays a coin value (possibly one that updates). Styled as inline.
 */
public class CoinLabel extends ValueHTML<Integer>
{
    /**
     * Returns HTML that can be used to render the specified quantity of coins.
     */
    public static String getCoinHTML (int coins)
    {
        return "<span style=\"white-space: nowrap\">" +
            "<img style=\"vertical-align: bottom\" src=\"images/money.png\">" + coins + "</span>";
    }

    /**
     * Creates a coin label with a non-changing coin value.
     */
    public CoinLabel (int coins)
    {
        this("", coins);
    }

    /**
     * Creates a coin label with a non-changing coin value and the supplied prefix label.
     */
    public CoinLabel (String label, int coins)
    {
        this(label, new Value<Integer>(coins));
    }

    /**
     * Creates a coin label with a dynamically changing coin value.
     */
    public CoinLabel (Value<Integer> coins)
    {
        this("", coins);
    }

    /**
     * Creates a coin label with a dynamically changing coin value and the supplied prefix label.
     */
    public CoinLabel (String label, Value<Integer> coins)
    {
        super(coins, "machine", "inline");
        _label = label;
    }

    @Override // from ValueHTML<Integer>
    protected String getHTML (Integer coins)
    {
        return _label + getCoinHTML(coins);
    }

    protected String _label;
}
