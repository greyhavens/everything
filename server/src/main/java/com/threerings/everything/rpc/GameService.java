//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

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
    /** The path at which this servlet is mapped. */
    public static final String ENTRY_POINT = "game";

    /** Thrown by {@link #getCollection} if the user in question does not exist. */
    public static final String E_UNKNOWN_USER = "e.unknown_user";

    /** Thrown by {@link #getSeries} if the series in question does not exist. */
    public static final String E_UNKNOWN_SERIES = "e.unknown_series";

    /** Thrown by {@link #getGrid} if the player has too few series for the ALL_COLLECTED_SERIES
     * powerup. */
    public static final String E_TOO_FEW_SERIES = "e.too_few_series";

    /** Thrown by {@link #flipCard} or {@link #usePowerup} if the grid in question has expired. */
    public static final String E_GRID_EXPIRED = "e.grid_expired";

    /** Thrown by {@link #flipCard} if the position requested has already been flipped. */
    public static final String E_ALREADY_FLIPPED = "e.already_flipped";

    /** Thrown by {@link #flipCard} if the user thinks they get a free flip but don't have one. */
    public static final String E_LACK_FREE_FLIP = "e.lack_free_flip";

    /** Thrown by {@link #flipCard} if the user's expected flip cost doesn't match the server's. */
    public static final String E_FLIP_COST_CHANGED = "e.flip_cost_changed";

    /** Thrown by {@link #flipCard} if the user can't afford the flip they requested. */
    public static final String E_NSF_FOR_FLIP = "e.nsf_for_flip";

    /** Thrown by various card-related methods if the card in question does not exist. */
    public static final String E_UNKNOWN_CARD = "e.unknown_card";

    /** Thrown by {@link #buyPowerup} if the powerup is permanent and the player already owns it. */
    public static final String E_ALREADY_OWN_POWERUP = "e.already_own_powerup";

    /** Thrown by {@link #buyPowerup} if the user can't afford the powerup they requested. */
    public static final String E_NSF_FOR_PURCHASE = "e.nsf_for_purchase";

    /** Thrown by {@link #usePower} if the user doesn't have a charge of the specified powerup. */
    public static final String E_LACK_CHARGE = "e.lack_charge";

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
