//
// $Id$

package com.threerings.everything.server.credits;

/**
 * The base class for JSON records sent to the Facebook Credits backend in response to payment
 * related requests and notifications.
 */
public class CreditsResponse
{
    /** We have to mimic the request method back to Facebook in the response, so this contains the
     * 'method' provided by Facebook in the request/notification. */
    public String method;

    public CreditsResponse (String method)
    {
        this.method = method;
    }
}
