//
// $Id$

package com.threerings.everything.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Grid;
import com.threerings.everything.data.PlayerCollection;
import com.threerings.everything.data.Powerup;
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
    void getCard (CardIdent cardId, AsyncCallback<Card> callback);

    /**
     * The async version of {@link GameService#getGrid}.
     */
    void getGrid (Powerup pup, boolean expectHave, AsyncCallback<GameService.GridResult> callback);

    /**
     * The async version of {@link GameService#flipCard}.
     */
    void flipCard (int gridId, int position, int expectedCost, AsyncCallback<GameService.FlipResult> callback);

    /**
     * The async version of {@link GameService#sellCard}.
     */
    void sellCard (int thingId, long created, AsyncCallback<Integer> callback);

    /**
     * The async version of {@link GameService#getGiftCardInfo}.
     */
    void getGiftCardInfo (int thingId, long created, AsyncCallback<GameService.GiftInfoResult> callback);

    /**
     * The async version of {@link GameService#giftCard}.
     */
    void giftCard (int thingId, long created, int friendId, String message, AsyncCallback<Void> callback);

    /**
     * The async version of {@link GameService#bonanzaViewed}.
     */
    void bonanzaViewed (boolean posted, AsyncCallback<GameStatus> callback);

    /**
     * The async version of {@link GameService#getAttractor}.
     */
    void getAttractor (int thingId, int friendId, AsyncCallback<GameService.CardResult> callback);

    /**
     * The async version of {@link GameService#openGift}.
     */
    void openGift (int thingId, long created, AsyncCallback<GameService.GiftResult> callback);

    /**
     * The async version of {@link GameService#getShopInfo}.
     */
    void getShopInfo (AsyncCallback<GameService.ShopResult> callback);

    /**
     * The async version of {@link GameService#buyPowerup}.
     */
    void buyPowerup (Powerup type, AsyncCallback<Void> callback);

    /**
     * The async version of {@link GameService#usePowerup}.
     */
    void usePowerup (int gridId, Powerup type, AsyncCallback<Grid> callback);
}
