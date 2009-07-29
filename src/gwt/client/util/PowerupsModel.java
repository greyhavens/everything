//
// $Id$

package client.util;

import java.util.HashMap;
import java.util.Map;

import com.threerings.gwt.util.Value;

import com.threerings.everything.data.Powerup;

/**
 * Contains information on the player's current powerup holdings.
 */
public class PowerupsModel
{
    public PowerupsModel (Map<Powerup, Integer> powerups)
    {
        refresh(powerups);
    }

    /**
     * Returns true if we have any pre-grid powerups.
     */
    public boolean havePreGrid ()
    {
        for (Powerup pup : Powerup.PRE_GRID) {
            if (getCharges(pup).get() > 0) {
                return true;
            }
        }
        return false;
    }

    public Value<Integer> getCharges (Powerup type)
    {
        Value<Integer> charges = _powerups.get(type);
        if (charges == null) {
            _powerups.put(type, charges = new Value<Integer>(0));
        }
        return charges;
    }

    public void notePurchase (Powerup type)
    {
        Value<Integer> charges = getCharges(type);
        charges.update(charges.get() + type.charges);
    }

    public void refresh (Map<Powerup, Integer> powerups)
    {
        for (Map.Entry<Powerup, Integer> entry : powerups.entrySet()) {
            getCharges(entry.getKey()).update(entry.getValue());
        }
    }

    protected Map<Powerup, Value<Integer>> _powerups = new HashMap<Powerup, Value<Integer>>();
}
