//
// $Id$

package com.threerings.everything.server.credits;

/**
 * A JSON record sent to Facebook containing the description of items to be sold for Facebook
 * Credits.
 */
public class ItemDescription extends CreditsResponse
{
    public static final String METHOD_NAME = "payments_get_items";

    public CoinsItem[] content;

    public ItemDescription (CoinsItem item)
    {
        super(METHOD_NAME);
        content = new CoinsItem[]{ item };
    }
}
