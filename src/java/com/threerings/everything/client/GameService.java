//
// $Id$

package com.threerings.everything.client;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.samsara.app.client.ServiceException;

import com.threerings.everything.data.Card;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Grid;

/**
 * Provides game services to the client.
 */
@RemoteServiceRelativePath(GameService.ENTRY_POINT)
public interface GameService extends RemoteService
{
    /** The path at which this servlet is mapped. */
    public static final String ENTRY_POINT = "game";

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

    /**
     * Returns the player's current grid.
     */
    GridResult getGrid () throws ServiceException;

    /**
     * Requests to flip the card at the specified position in the specified grid.
     */
    FlipResult flipCard (int gridId, int position, int expectedCost) throws ServiceException;

    /**
     * Returns detail information for the specified card, or null if the specified player does not
     * own that card.
     */
    Card getCard (int ownerId, int thingId, long created) throws ServiceException;
}
