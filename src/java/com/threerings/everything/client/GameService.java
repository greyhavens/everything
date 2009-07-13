//
// $Id$

package com.threerings.everything.client;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.samsara.app.client.ServiceException;

import com.threerings.everything.data.Card;
import com.threerings.everything.data.FriendCardInfo;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Grid;
import com.threerings.everything.data.PlayerCollection;
import com.threerings.everything.data.Series;

/**
 * Provides game services to the client.
 */
@RemoteServiceRelativePath(GameService.ENTRY_POINT)
public interface GameService extends RemoteService
{
    /** The path at which this servlet is mapped. */
    public static final String ENTRY_POINT = "game";

    /** Thrown by {@link #getCollection} if the user in question does not exist. */
    public static final String E_UNKNOWN_USER = "e.unknown_user";

    /** Thrown by {@link #getSeries} if the user in series does not exist. */
    public static final String E_UNKNOWN_SERIES = "e.unknown_series";

    /** Thrown by {@link #flipCard} if the grid in question has expired. */
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

    /** Provides results for {@link #getGrid}. */
    public static class GridResult implements IsSerializable
    {
        /** The player's current grid. */
        public Grid grid;

        /** The player's current game status. */
        public GameStatus status;
    }

    /** Provides results for {@link #flipCard}. */
    public class FlipResult implements IsSerializable
    {
        /** The card at the position they flipped. */
        public Card card;

        /** The player's new game status after the flip. */
        public GameStatus status;
    }

    /** Provides results for {@link #getGiftCardInfo}. */
    public class GiftInfoResult implements IsSerializable
    {
        /** The number of things in the series of the card being considered for gifting. */
        public int things;

        /** The status of each of this player's friends that do not already have the card. */
        public List<FriendCardInfo> friends;
    }

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
    Card getCard (int ownerId, int thingId, long created) throws ServiceException;

    /**
     * Returns the player's current grid.
     */
    GridResult getGrid () throws ServiceException;

    /**
     * Requests to flip the card at the specified position in the specified grid.
     */
    FlipResult flipCard (int gridId, int position, int expectedCost) throws ServiceException;

    /**
     * Requests to sell the specified card. Returns the caller's new coin balance.
     */
    int sellCard (int thingId, long created) throws ServiceException;

    /**
     * Requests data on this player's friends who do not already have the specified card.
     */
    GiftInfoResult getGiftCardInfo (int thingId, long created) throws ServiceException;

    /**
     * Gifts the specified card to the specified friend.
     */
    void giftCard (int thingId, long created, int friendId) throws ServiceException;
}
