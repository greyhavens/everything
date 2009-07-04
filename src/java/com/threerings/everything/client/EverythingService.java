//
// $Id$

package com.threerings.everything.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.samsara.app.client.ServiceException;

import com.threerings.everything.data.Card;

/**
 * Defines the services available to the Everything client.
 */
@RemoteServiceRelativePath(EverythingService.ENTRY_POINT)
public interface EverythingService extends RemoteService
{
    /** The path at which this servlet is mapped. */
    public static final String ENTRY_POINT = "everything";

    /**
     * Returns detail information for the specified card, or null if the specified player does not
     * own a card with the specified thing.
     */
    Card getCard (int ownerId, int thingId) throws ServiceException;
}
