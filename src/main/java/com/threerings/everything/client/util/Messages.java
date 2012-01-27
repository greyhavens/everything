//
// $Id$

package com.threerings.everything.client.util;

import java.util.MissingResourceException;

import com.google.gwt.core.client.GWT;

/**
 * Handles the translation of non-static messages.
 */
public class Messages
{
    /**
     * Translates the supplied code into a human readable message.
     */
    public static String xlate (String code)
    {
        try {
            return _dmsgs.xlate(code);
        } catch (MissingResourceException mre) {
            return code; // return the raw string
        }
    }

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
