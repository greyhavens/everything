//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.client.util;

import java.util.HashMap;
import java.util.Map;

import com.threerings.gwt.util.CookieUtil;

/**
 * Stores some client-side preferences.
 */
public class Prefs
{
    /** Whether to check the "post to feed" when gifting a card by default. */
    public static final String GIFT_POST = "g";

    /**
     * Returns true if the specified preference value is set, false if not.
     */
    public static boolean get (String key, boolean defval)
    {
        String val = decode().get(key);
        return (val == null) ? defval : "t".equals(val);
    }

    /**
     * Configures the specified boolean preference.
     */
    public static void set (String key, boolean value)
    {
        Map<String, String> prefs = decode();
        prefs.put(key, value ? "t" : "f");
        update(prefs);
    }

    protected static Map<String, String> decode ()
    {
        Map<String, String> prefs = new HashMap<String, String>();
        String data = CookieUtil.get(PREFS_COOKIE);
        if (data != null) {
            for (String pref : data.split("&")) {
                int eidx = pref.indexOf("=");
                if (eidx == -1) {
                    prefs.put(pref, "");
                } else {
                    prefs.put(pref.substring(0, eidx), pref.substring(eidx+1));
                }
            }
        }
        return prefs;
    }

    protected static void update (Map<String, String> prefs)
    {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, String> entry : prefs.entrySet()) {
            buf.append(entry.getKey());
            if (entry.getValue().length() > 0) {
                buf.append("=").append(entry.getValue());
            }
        }
        CookieUtil.set("/", 365, PREFS_COOKIE, buf.toString());
    }

    protected static final String PREFS_COOKIE = "p";
}
