//
// $Id$

package com.threerings.everything.server;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.server.AppServiceServlet;

import com.threerings.everything.client.GameService;
import com.threerings.everything.data.Card;

import static com.threerings.everything.Log.log;

/**
 * Implements {@link GameService}.
 */
public class GameServlet extends AppServiceServlet
    implements GameService
{
    // from interface GameService
    public Card getCard (int ownerId, int thingId) throws ServiceException
    {
        return null; // TODO
    }
}
