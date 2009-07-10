//
// $Id$

package com.threerings.everything.server;

import com.google.inject.Inject;
import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.AppServiceServlet;

import com.threerings.everything.client.GameService;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.Grid;
import com.threerings.everything.server.persist.CardRecord;
import com.threerings.everything.server.persist.GameRepository;
import com.threerings.everything.server.persist.GridRecord;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;

import static com.threerings.everything.Log.log;

/**
 * Implements {@link GameService}.
 */
public class GameServlet extends EveryServiceServlet
    implements GameService
{
    // from interface GameService
    public GridResult getGrid () throws ServiceException
    {
        PlayerRecord player = requirePlayer();

        GridRecord grid = _gameRepo.loadGrid(player.userId);
        if (grid == null || grid.expires.getTime() < System.currentTimeMillis()) {
            // generate a new grid
            grid = _gameLogic.generateGrid(player, grid);

            // store the new grid in ze database and reset the player's flipped status
            _gameRepo.storeGrid(grid);
            _gameRepo.resetFlipped(player.userId);

            log.info("Generated grid", "for", player.who(), "things", grid.thingIds,
                     "expires", grid.expires);
        }

        GridResult result = new GridResult();
        result.grid = _gameLogic.resolveGrid(grid);
        result.status = _gameLogic.getGameStatus(player, result.grid.unflipped);
        return result;
    }

    // from interface GameService
    public FlipResult flipCard (int gridId, int position, int expectedCost) throws ServiceException
    {
        PlayerRecord player = requirePlayer();

        // load up the grid they're flipping
        GridRecord grec = _gameRepo.loadGrid(player.userId);
        if (grec == null || grec.gridId != gridId) {
            throw new ServiceException(E_GRID_EXPIRED);
        }

        // compute the cost of this flip
        Grid grid = _gameLogic.resolveGrid(grec);
        int flipCost = _gameLogic.getNextFlipCost(grid.unflipped);

        // make sure they look like they can afford it (or have a freebie)
        checkCanPayForFlip(player, flipCost, expectedCost);

        // mark this position as flipped in the player's grid
        if (!_gameRepo.flipPosition(player.userId, position)) {
            throw new ServiceException(E_ALREADY_FLIPPED);
        }

        // actually pay for the flip (which may fail because we had out of date info)
        try {
            payForFlip(player, flipCost, expectedCost);
        } catch (ServiceException se) {
            _gameRepo.resetPosition(player.userId, position);
            throw se;
        }

        // create the card and add it to the player's collection
        CardRecord card = _gameRepo.createCard(player.userId, grec.thingIds[position]);

        // resolve the runtime data for the card and report our result
        FlipResult result = new FlipResult();
        result.card = _gameLogic.resolveCard(card);
        // decrement the unflipped count for the flipped card's rarity so that we can properly
        // compute the new next flip cost
        grid.unflipped[result.card.thing.rarity.ordinal()]--;
        result.status = _gameLogic.getGameStatus(
            _playerRepo.loadPlayer(player.userId), grid.unflipped);

        log.info("Yay! Card flipped", "who", player.who(), "thing", result.card.thing.name,
                 "rarity", result.card.thing.rarity, "paid", expectedCost);
        return result;
    }

    // from interface GameService
    public Card getCard (int ownerId, int thingId, long created) throws ServiceException
    {
        // TODO: show less info if the caller is not the owner?
        CardRecord card = _gameRepo.loadCard(ownerId, thingId, created);
        return (card == null) ? null : _gameLogic.resolveCard(card);
    }

    protected void checkCanPayForFlip (PlayerRecord player, int flipCost, int expectedCost)
        throws ServiceException
    {
        if (player.freeFlips >= 1) {
            return;
        }
        if (expectedCost != flipCost) {
            throw new ServiceException(E_FLIP_COST_CHANGED);
        }
        if (player.coins < flipCost) {
            throw new ServiceException(E_NSF_FOR_FLIP);
        }
    }

    protected void payForFlip (PlayerRecord player, int flipCost, int expectedCost)
        throws ServiceException
    {
        // if they have a free flip, always try to use it
        if (player.freeFlips >= 1) {
            if (_playerRepo.consumeFreeFlip(player.userId)) {
                return; // great, all done
            } else if (expectedCost == 0) {
                // they thought they were getting a free flip, but don't have one
                throw new ServiceException(E_LACK_FREE_FLIP);
            }
        }

        // re-check that the expected cost matches
        if (expectedCost != flipCost) {
            throw new ServiceException(E_FLIP_COST_CHANGED);
        }

        // deduct the coins from the player's account
        if (!_playerRepo.consumeCoins(player.userId, expectedCost)) {
            throw new ServiceException(E_NSF_FOR_FLIP);
        }
    }

    @Inject protected GameLogic _gameLogic;
    @Inject protected GameRepository _gameRepo;
}
