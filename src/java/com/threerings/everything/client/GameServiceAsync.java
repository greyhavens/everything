//
// $Id$

package com.threerings.everything.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.everything.data.Card;
import com.threerings.everything.data.Grid;
import com.threerings.everything.data.PlayerCollection;
import com.threerings.everything.data.Series;

/**
 * Provides the asynchronous version of {@link GameService}.
 */
public interface GameServiceAsync
{
    /**
     * The async version of {@link GameService#getCollection}.
     */
    void getCollection (int ownerId, AsyncCallback<PlayerCollection> callback);

    /**
     * The async version of {@link GameService#getSeries}.
     */
    void getSeries (int ownerId, int categoryId, AsyncCallback<Series> callback);

    /**
     * The async version of {@link GameService#getCard}.
     */
    void getCard (int playerId, int thingId, long created, AsyncCallback<Card> callback);

    /**
     * The async version of {@link GameService#getGrid}.
     */
    void getGrid (AsyncCallback<GameService.GridResult> callback);

    /**
     * The async version of {@link GameService#flipCard}.
     */
    void flipCard (int gridId, int position, int expectedCost,
                   AsyncCallback<GameService.FlipResult> callback);

    /**
     * The async version of {@link GameService#sellCard}.
     */
    void sellCard (int thingId, long created, AsyncCallback<Integer> callback);
}
