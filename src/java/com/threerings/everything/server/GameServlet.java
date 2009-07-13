//
// $Id$

package com.threerings.everything.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntMap;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.AppServiceServlet;

import com.threerings.everything.client.GameService;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.FriendCardInfo;
import com.threerings.everything.data.Grid;
import com.threerings.everything.data.PlayerCollection;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.Series;
import com.threerings.everything.data.SeriesCard;
import com.threerings.everything.data.Thing;
import com.threerings.everything.data.ThingCard;
import com.threerings.everything.server.persist.CardRecord;
import com.threerings.everything.server.persist.GameRepository;
import com.threerings.everything.server.persist.GridRecord;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;
import com.threerings.everything.server.persist.ThingRepository;

import static com.threerings.everything.Log.log;

/**
 * Implements {@link GameService}.
 */
public class GameServlet extends EveryServiceServlet
    implements GameService
{
    // from interface GameService
    public PlayerCollection getCollection (int ownerId) throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        // TODO: require that the caller be a friend of owner?

        PlayerCollection coll = new PlayerCollection();
        coll.owner = _playerRepo.loadPlayerName(ownerId);
        if (coll.owner == null) {
            throw new ServiceException(E_UNKNOWN_USER);
        }

        // first load up all of the series
        Multimap<Integer, SeriesCard> series = HashMultimap.create();
        for (SeriesCard card : _thingRepo.loadPlayerSeries(ownerId)) {
            series.put(card.parentId, card);
        }

        // load up the series' parents, the sub-categories
        Multimap<Integer, Category> subcats = HashMultimap.create();
        for (Category subcat : _thingRepo.loadCategories(series.keySet())) {
            subcats.put(subcat.parentId, subcat);
        }

        // finally load up the top-level categories and build everything back down
        coll.series = Maps.newHashMap();
        for (Category cat : _thingRepo.loadCategories(subcats.keySet())) {
            Map<String, List<SeriesCard>> scats = Maps.newHashMap();
            for (Category scat : subcats.get(cat.categoryId)) {
                scats.put(scat.name, Lists.newArrayList(series.get(scat.categoryId)));
            }
            coll.series.put(cat.name, scats);
        }

        return coll;
    }

    // from interface GameService
    public Series getSeries (int ownerId, int categoryId) throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        // TODO: require that the caller be a friend of owner?

        Category category = _thingRepo.loadCategory(categoryId);
        if (category == null) {
            throw new ServiceException(E_UNKNOWN_SERIES);
        }

        // load up the cards owned by this player
        List<CardRecord> crecs = _gameRepo.loadCards(ownerId, categoryId);
        List<ThingCard> cards = Lists.newArrayList();

        // then load up all things in the series so that we can fill in blanks
        for (ThingCard thing : Sets.newTreeSet(_thingRepo.loadThingCards(categoryId))) {
            boolean added = false;
            for (CardRecord crec : crecs) {
                if (crec.thingId == thing.thingId) {
                    ThingCard card = ThingCard.clone(thing);
                    card.created = crec.created.getTime();
                    cards.add(card);
                    added = true;
                }
            }
            if (!added) {
                cards.add(null); // add a blank spot for this thing
            }
        }

        Series series = new Series();
        series.categoryId = categoryId;
        series.name = category.name;
        series.things = cards.toArray(new ThingCard[cards.size()]);
        return series;
    }

    // from interface GameService
    public Card getCard (CardIdent ident) throws ServiceException
    {
        // TODO: show less info if the caller is not the owner?
        return _gameLogic.resolveCard(requireCard(ident.ownerId, ident.thingId, ident.created));
    }

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

        log.info("Returning grid", "for", player.who(), "things", grid.thingIds);
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

        // count up the number of the selected thing already owned by this player before we create
        // this new card
        FlipResult result = new FlipResult();
        result.haveCount = _gameRepo.countCardHoldings(player.userId, grec.thingIds[position]);

        // create the card and add it to the player's collection
        CardRecord card = _gameRepo.createCard(player.userId, grec.thingIds[position]);

        // resolve the runtime data for the card and report our result
        result.card = _gameLogic.resolveCard(card);
        // decrement the unflipped count for the flipped card's rarity so that we can properly
        // compute the new next flip cost
        grid.unflipped[result.card.thing.rarity.ordinal()]--;
        result.status = _gameLogic.getGameStatus(
            _playerRepo.loadPlayer(player.userId), grid.unflipped);

        // record that this player flipped this card
        _playerRepo.recordFeedItem(player.userId, FeedItem.Type.FLIPPED, 0, result.card.thing.name);

        log.info("Yay! Card flipped", "who", player.who(), "thing", result.card.thing.name,
                 "rarity", result.card.thing.rarity, "paid", expectedCost);
        return result;
    }

    // from interface GameService
    public int sellCard (int thingId, long created) throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        CardRecord card = requireCard(player.userId, thingId, created);

        // players receive half the value of the thing for cashing in a card
        int coins = _thingRepo.loadThing(card.thingId).rarity.saleValue();

        // we grant the coins and then delete the card; it's possible that this means that a
        // database failure will result in them getting money and keeping their card but it seems
        // quite unlikely that they'll get to take advantage of such a failure more than once...
        _playerRepo.grantCoins(player.userId, coins);
        _gameRepo.deleteCard(card);

        // return their new coin balance (this doesn't have to be absolutely correct)
        return player.coins + coins;
    }

    // from interface GameService
    public GiftInfoResult getGiftCardInfo (int thingId, long created) throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        CardRecord card = requireCard(player.userId, thingId, created);

        // load up data on the things in this set
        Set<Integer> thingIds = Sets.newHashSet();
        for (ThingCard thing : _thingRepo.loadThingCards(
                 _thingRepo.loadThing(thingId).categoryId)) {
            thingIds.add(thing.thingId);
        }

        // determine which friends do not have a card with this thing on it
        Set<Integer> friendIds = Sets.newHashSet(_playerRepo.loadFriendIds(player.userId));
        friendIds.removeAll(_gameRepo.countCardHoldings(
                                friendIds, Collections.singleton(thingId)).keySet());

        // count up how many of the cards in this series are held by these friends
        IntIntMap holdings = _gameRepo.countCardHoldings(friendIds, thingIds);

        // load up the names of those friends
        IntMap<PlayerName> names = _playerRepo.loadPlayerNames(friendIds);

        // TODO: load up wishlist info on this thing

        GiftInfoResult result = new GiftInfoResult();
        result.things = thingIds.size();
        result.friends = Lists.newArrayList();
        for (Integer friendId : friendIds) {
            FriendCardInfo info = new FriendCardInfo();
            info.friend = names.get(friendId);
            info.hasThings = holdings.getOrElse(friendId, 0);
            info.onWishlist = false; // TODO
            result.friends.add(info);
        }
        return result;
    }

    // from interface GameService
    public void giftCard (int thingId, long created, int friendId) throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        CardRecord card = requireCard(player.userId, thingId, created);

        // transfer the card to the target player
        _gameRepo.giftCard(card, friendId);

        // record that this player gifted this card
        Thing thing = _thingRepo.loadThing(thingId);
        _playerRepo.recordFeedItem(player.userId, FeedItem.Type.GIFTED, friendId, thing.name);
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

    protected CardRecord requireCard (int ownerId, int thingId, long created)
        throws ServiceException
    {
        CardRecord card = _gameRepo.loadCard(ownerId, thingId, created);
        if (card == null) {
            throw new ServiceException(E_UNKNOWN_CARD);
        }
        return card;
    }

    @Inject protected GameLogic _gameLogic;
    @Inject protected GameRepository _gameRepo;
    @Inject protected ThingRepository _thingRepo;
}
