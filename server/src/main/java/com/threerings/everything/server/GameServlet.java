//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server;

import java.sql.Timestamp;

import com.google.inject.Inject;

import com.threerings.app.client.ServiceException;

import com.threerings.everything.data.Card;
import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Grid;
import com.threerings.everything.data.PlayerCollection;
import com.threerings.everything.data.Powerup;
import com.threerings.everything.data.Series;
import com.threerings.everything.rpc.GameService;
import com.threerings.everything.server.persist.GameRepository;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;
import com.threerings.everything.util.GameUtil;

/**
 * Implements {@link GameService}.
 */
public class GameServlet extends EveryServiceServlet
    implements GameService
{
    // from interface GameService
    public PlayerCollection getCollection (int ownerId) throws ServiceException
    {
        return _collLogic.getCollection(ownerId);
    }

    // from interface GameService
    public Series getSeries (int ownerId, int categoryId) throws ServiceException
    {
        return _collLogic.getSeries(ownerId, categoryId);
    }

    // from interface GameService
    public Card getCard (CardIdent ident) throws ServiceException
    {
        return _cardLogic.getCard(ident);
    }

    // from interface GameService
    public GridResult getGrid (Powerup pup, boolean expectHave) throws ServiceException
    {
        return _gameLogic.getGrid(requirePlayer(), pup, expectHave);
    }

    // from interface GameService
    public FlipResult flipCard (int gridId, int position, int expectedCost) throws ServiceException
    {
        return _cardLogic.flipCard(requirePlayer(), gridId, position, expectedCost);
    }

    // from interface GameService
    public SellResult sellCard (int thingId, long received) throws ServiceException
    {
        return _cardLogic.sellCard(requirePlayer(), thingId, received);
    }

    // from interface GameService
    public GiftInfoResult getGiftCardInfo (int thingId, long received) throws ServiceException
    {
        return _cardLogic.getGiftCardInfo(requirePlayer(), thingId, received);
    }

    // from interface GameService
    public void giftCard (int thingId, long received, int friendId, String message)
        throws ServiceException
    {
        _cardLogic.giftCard(requirePlayer(), thingId, received, friendId, message);
    }

    // from interface GameService
    public void setLike (int categoryId, Boolean like) throws ServiceException
    {
        _collLogic.setLike(requirePlayer(), categoryId, like);
    }

    // from interface GameService
    public GameStatus bonanzaViewed (int thingId)
        throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        if (!player.eligibleForAttractor()) {
            throw ServiceException.internalError(); // TODO: better error?
        }
        boolean posted = (thingId > 0);
        // if they posted the attractor, give them another one 2 days from now, otherwise 5 days..
        _playerRepo.setNextAttractor(player,
            new Timestamp(player.calculateNextExpires().getTime() +
                ((posted ? 1 : 4) * GameUtil.ONE_DAY)));
        if (!posted) {
            return null; // that's it, they turned us down, screw them.
        }
        _gameRepo.notePostedAttractor(player.userId, thingId);
        // otherwise, grant them a free flip
        _playerRepo.grantFreeFlips(player, 1);
        // return their game status
        return _gameLogic.getGameStatus(player, null /* unflipped */);
        // (Note: we pass null for 'unflipped' because it won't be used since we know for a fact
        // that they have at least one free flip (we just granted it!))
    }

    // from interface GameService
    public CardResult getAttractor (int thingId, int friendId)
        throws ServiceException
    {
        return _cardLogic.getAttractor(requirePlayer(), thingId, friendId);
    }

    // from interface GameService
    public GiftResult openGift (int thingId, long created)
        throws ServiceException
    {
        return _cardLogic.openGift(requirePlayer(), thingId, created);
    }

    // from interface GameService
    public ShopResult getShopInfo () throws ServiceException
    {
        return _gameLogic.getShopInfo(requirePlayer());
    }

    // from interface GameService
    public void buyPowerup (Powerup pup) throws ServiceException
    {
        _gameLogic.buyPowerup(requirePlayer(), pup);
    }

    // from interface GameService
    public Grid usePowerup (int gridId, Powerup pup) throws ServiceException
    {
        return _gameLogic.usePowerup(requirePlayer(), gridId, pup);
    }

//    protected String feedReplacements (String source, PlayerRecord player)
//    {
//        // for now we simply replace "%n" with the first name
//        return source.replace("%n", player.name);
//    }

    @Inject protected GameLogic _gameLogic;
    @Inject protected CollectionLogic _collLogic;
    @Inject protected CardLogic _cardLogic;
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected GameRepository _gameRepo;
}
