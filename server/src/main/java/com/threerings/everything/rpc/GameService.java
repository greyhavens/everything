//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.rpc;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.app.client.ServiceException;

import com.threerings.everything.data.Card;
import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Grid;
import com.threerings.everything.data.PlayerCollection;
import com.threerings.everything.data.Powerup;
import com.threerings.everything.data.Series;

/**
 * Provides game services to the client.
 */
@RemoteServiceRelativePath(GameService.ENTRY_POINT)
public interface GameService extends RemoteService, GameAPI
{
    /**
     * Returns the collection of the specified player.
     */
    PlayerCollection getCollection (int ownerId) throws ServiceException;

    /**
     * Returns the series data for the specified player's series.
     */
    Series getSeries (int ownerId, int categoryId) throws ServiceException;

    /**
     * Returns detail information for the specified card.
     */
    Card getCard (CardIdent cardId) throws ServiceException;

    /**
     * Returns the player's current grid.
     */
    GridResult getGrid (Powerup pup, boolean expectHave) throws ServiceException;

    /**
     * Requests to flip the card at the specified position in the specified grid.
     */
    FlipResult flipCard (int gridId, int position, int expectedCost) throws ServiceException;

    /**
     * Requests to sell the specified card. Returns the caller's new coin balance.
     */
    SellResult sellCard (int thingId, long created) throws ServiceException;

    /**
     * Requests data on this player's friends who do not already have the specified card.
     */
    GiftInfoResult getGiftCardInfo (int thingId, long created) throws ServiceException;

    /**
     * Gifts the specified card to the specified friend.
     */
    void giftCard (int thingId, long created, int friendId, String message) throws ServiceException;

    /**
     * Sets whether the player likes the specified category.
     */
    void setLike (int categoryId, Boolean like) throws ServiceException;

    /**
     * Notify the server that we've posted or skipped (thingId=-1) after seeing a bonanza card.
     * Returns the caller's current free flip count.
     */
    GameStatus bonanzaViewed (int thingId) throws ServiceException;

    /**
     * Add an attractor card to the player's collection, if they don't already have it.
     */
    CardResult getAttractor (int thingId, int friendId) throws ServiceException;

    /**
     * Opens a card gift.
     */
    GiftResult openGift (int thingId, long created) throws ServiceException;

    /**
     * Returns data needed to display the shop.
     */
    ShopResult getShopInfo () throws ServiceException;

    /**
     * Purchases the specified powerup.
     */
    void buyPowerup (Powerup type) throws ServiceException;

    /**
     * Uses a powerup on the specified grid. Returns the grid with the new status.
     */
    Grid usePowerup (int gridId, Powerup type) throws ServiceException;
}
