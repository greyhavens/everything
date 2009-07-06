//
// $Id$

package com.threerings.everything.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.samsara.app.client.ServiceException;

import com.threerings.everything.data.Card;

/**
 * Provides game services to the client.
 */
@RemoteServiceRelativePath(GameService.ENTRY_POINT)
public interface GameService extends RemoteService
{
    /** The path at which this servlet is mapped. */
    public static final String ENTRY_POINT = "game";

    /**
     * Returns detail information for the specified card, or null if the specified player does not
     * own a card with the specified thing.
     */
    Card getCard (int ownerId, int thingId) throws ServiceException;
}
