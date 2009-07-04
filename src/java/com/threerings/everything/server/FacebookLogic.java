//
// $Id$

package com.threerings.everything.server;

import java.net.URL;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.google.code.facebookapi.FacebookJaxbRestClient;

/**
 * Centralizes some Facebook API bits.
 */
@Singleton
public class FacebookLogic
{
    /**
     * Returns a Facebook client not bound to any particular user's session.
     */
    public FacebookJaxbRestClient getFacebookClient ()
    {
        return getFacebookClient((String)null);
    }

    /**
     * Returns a Facebook client bound to the supplied user's session.
     */
    public FacebookJaxbRestClient getFacebookClient (String sessionKey)
    {
        return new FacebookJaxbRestClient(
            SERVER_URL, _app.getFacebookKey(), _app.getFacebookSecret(), sessionKey,
            CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    @Inject protected EverythingApp _app;

    protected static final int CONNECT_TIMEOUT = 15*1000; // in millis
    protected static final int READ_TIMEOUT = 15*1000; // in millis

    protected static final URL SERVER_URL;
    static {
        try {
            SERVER_URL = new URL("http://api.facebook.com/restserver.php");
        } catch (Exception e) {
            throw new RuntimeException(e); // MalformedURLException should be unchecked, sigh
        }
    }
}
