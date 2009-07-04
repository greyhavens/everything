//
// $Id$

package com.threerings.everything.server;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.server.AppServiceServlet;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.data.Card;

/**
 * Implements {@link EverythingService}.
 */
public class EverythingServlet extends AppServiceServlet
    implements EverythingService
{
    // from interface EverythingService
    public Card getCard (int ownerId, int thingId) throws ServiceException
    {
        return null; // TODO
    }
}
