//
// $Id$

package com.threerings.everything.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.everything.data.Card;
import com.threerings.everything.data.SessionData;

/**
 * Provides the asynchronous version of {@link EverythingService}.
 */
public interface EverythingServiceAsync
{
    /**
     * The async version of {@link EverythingService#validateSession}.
     */
    void validateSession (AsyncCallback<SessionData> callback);

    /**
     * The async version of {@link EverythingService#getCard}.
     */
    void getCard (int playerId, int thingId, AsyncCallback<Card> callback);
}
