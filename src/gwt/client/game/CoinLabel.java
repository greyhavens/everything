//
// $Id$

package client.game;

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
        return "&copy;" + coins;
    }

    /**
     * Creates a coin label with a non-changing coin value.
     */
    public CoinLabel (int coins)
    {
        this(new Value<Integer>(coins));
    }

    /**
     * Creates a coin label with a dynamically changing coin value.
     */
    public CoinLabel (Value<Integer> coins)
    {
        super(coins);
        addStyleName("inline");
    }

    @Override // from ValueHTML<Integer>
    protected String getHTML (Integer coins)
    {
        return getCoinHTML(coins);
    }
}
