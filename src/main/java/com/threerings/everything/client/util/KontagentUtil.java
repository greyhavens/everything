//
// $Id$

package com.threerings.everything.client.util;

import com.google.gwt.user.client.Random;

/**
 * Kontagent utility methods.
 */
public class KontagentUtil
{
    /**
     * Generates a unique id for use with Kontagent.
     */
    public static String generateUniqueId (int forUserId)
    {
        int stamp = (int)(System.currentTimeMillis() % Integer.MAX_VALUE);
        return toHex(stamp ^ Random.nextInt()) + toHex(forUserId);
    }

    protected static String toHex (int value)
    {
        String text = Integer.toHexString(value);
        while (text.length() < 8) {
            text = "0" + text;
        }
        return text;
    }
}
