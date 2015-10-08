//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Callable;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.app.client.ServiceException;

import com.threerings.everything.data.Build;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.FriendCardInfo;
import com.threerings.everything.data.Grid;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.SlotStatus;
import com.threerings.everything.data.Thing;
import com.threerings.everything.data.ThingCard;
import com.threerings.everything.data.TrophyData;
import com.threerings.everything.rpc.GameService;
import com.threerings.everything.server.persist.CardRecord;
import com.threerings.everything.server.persist.GameRepository;
import com.threerings.everything.server.persist.GridRecord;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;
import com.threerings.everything.server.persist.ThingRepository;
import com.threerings.everything.util.GameUtil;

import static com.threerings.everything.Log.log;

/**
 * Logic relating to flipping, selling, etc. cards.
 */
@Singleton
public class CardLogic {

    public Card getCard (CardIdent ident) throws ServiceException
    {
        return _gameLogic.resolveCard(requireCard(ident.ownerId, ident.thingId, ident.received));
    }

    public GameService.FlipResult flipCard (
        PlayerRecord player, int gridId, int position, int expectedCost) throws ServiceException
    {
        // load up the grid they're flipping
        GridRecord grec = _gameRepo.loadGrid(player.userId);
        if (grec == null || grec.gridId != gridId) {
            throw new ServiceException(GameService.E_GRID_EXPIRED);
        }

        // compute the cost of this flip
        Grid grid = _gameLogic.resolveGrid(grec);
        int flipCost = _gameLogic.getNextFlipCost(grid.unflipped);

        // make sure they look like they can afford it (or have a freebie)
        checkCanPayForFlip(player, flipCost, expectedCost);

        // mark this position as flipped in the player's grid
        if (!_gameRepo.flipSlot(player.userId, position)) {
            throw new ServiceException(GameService.E_ALREADY_FLIPPED);
        }

        // actually pay for the flip (which may fail because we had out of date info)
        try {
            payForFlip(player, flipCost, expectedCost);
        } catch (ServiceException se) {
            _gameRepo.resetSlot(player.userId, position);
            throw se;
        }

        // reload the player record to obtain an updated coin and free flip count
        player = _playerRepo.loadPlayer(player.userId);

        // load up the thing going on the card they just flipped
        Thing thing = _thingRepo.loadThing(grec.thingIds[position]);

        log.info("Yay! Card flipped", "who", player.who(), "thing", thing.name,
                 "rarity", thing.rarity, "paid", expectedCost);

        // create the card, add it to their collection and resolve associated bits
        GameService.FlipResult result = new GameService.FlipResult();
        CardRecord card = prepareCard(player, thing, result,
            cardCreator(player.userId, thing.thingId, 0));

        // decrement the unflipped count for the flipped card's rarity so that we can properly
        // compute the new next flip cost
        grid.unflipped[thing.rarity.ordinal()]--;
        result.status = _gameLogic.getGameStatus(player, grid.unflipped);

        // note the received timestamp of the created card in this position
        _gameRepo.updateSlot(player.userId, position, card.received.getTime());

        // record that this player flipped this card
        _playerRepo.recordFeedItem(player.userId, FeedItem.Type.FLIPPED, 0, thing.name);

        // on a free flip, there's a chance they'll get a bonanza card
        if (Build.bonanzasEnabled() && expectedCost == 0) {
            result.bonanza = maybePickBonanza(player, result.card);
        }

        return result;
    }

    public GameService.SellResult sellCard (PlayerRecord player, int thingId, long received)
        throws ServiceException
    {
        CardRecord card = requireCard(player.userId, thingId, received);

        // players receive half the value of the thing for cashing in a card
        Thing thing = _thingRepo.loadThing(card.thingId);
        int coins = thing.rarity.saleValue();

        // we grant the coins and then delete the card; it's possible that this means that a
        // database failure will result in them getting money and keeping their card but it seems
        // quite unlikely that they'll get to take advantage of such a failure more than once...
        _playerRepo.grantCoins(player.userId, coins);
        _gameRepo.deleteCard(card);
        log.info("Player sold card", "who", player.who(), "thing", thing.name, "coins", coins);

        // this card may be on their grid, in which case we need to update its status
        _gameLogic.noteCardStatus(card, SlotStatus.SOLD);

        // return their new coin balance (this doesn't have to be absolutely correct)
        GameService.SellResult result = new GameService.SellResult();
        result.coins = player.coins + coins;

        // If they sold the last card in a series, downgrade their 'like' preference
        Boolean like = _playerRepo.getLike(player.userId, thing.categoryId);
        if ((like == null || like == true) &&
                _gameRepo.loadCards(player.userId, thing.categoryId).isEmpty()) {
            like = (like == null) ? false : null; // downgrade
            _playerRepo.setLike(player.userId, thing.categoryId, like);
        }
        result.newLike = like;

        return result;
    }

    public GameService.GiftInfoResult getGiftCardInfo (
        PlayerRecord player, int thingId, long received) throws ServiceException
    {
        requireCard(player.userId, thingId, received);

        int categoryId = _thingRepo.loadThing(thingId).categoryId;

        // load up data on the things in this set
        Set<Integer> thingIds = Sets.newHashSet();
        for (ThingCard thing : _thingRepo.loadThingCards(categoryId)) {
            thingIds.add(thing.thingId);
        }

        // determine which friends do not have a card with this thing on it,
        // including friends who have it as a pending gift
        Set<Integer> friendIds = _playerRepo.loadFriendIds(player.userId).toSet();
        Multiset<Integer> holdings =
            _gameRepo.countCardHoldings(friendIds, Collections.singleton(thingId), true);
        friendIds.removeAll(holdings.elementSet());

        // Now count up how many of the cards in this series are held by these friends
        // Note that here we do NOT count pending gifts, as that may encourage a friend to
        // give you a second card in the same series when aren't *really* collecting it yet-
        // you may opt to sell the first gift and there's no reason to make it look like
        // you are a "collector" of that series...
        holdings = _gameRepo.countCardHoldings(friendIds, thingIds, false);
        // load up the names of those friends
        Map<Integer, PlayerName> names = _playerRepo.loadPlayerNames(friendIds);
        // load up the likes for these friends
        Map<Integer, Boolean> likes = _playerRepo.loadLikes(friendIds, categoryId);

        GameService.GiftInfoResult result = new GameService.GiftInfoResult();
        result.things = thingIds.size();
        result.friends = Lists.newArrayListWithCapacity(friendIds.size());
        for (Integer friendId : friendIds) {
            FriendCardInfo info = new FriendCardInfo();
            info.friend = names.get(friendId);
            info.hasThings = holdings.count(friendId);
            info.like = likes.get(friendId);
            result.friends.add(info);
        }
        return result;
    }

    public void giftCard (PlayerRecord player, int thingId, long received,
                          int friendId, String message) throws ServiceException
    {
        PlayerRecord friend = _playerRepo.loadPlayer(friendId);
        if (friend == null) {
            log.warning("Requested to gift to unknown friend?", "gifter", player.who(),
                        "friendId", friendId);
            throw ServiceException.internalError();
        }
        CardRecord card = requireCard(player.userId, thingId, received);
        _gameLogic.giftCard(player, card, friend, message, false);
    }

    // from interface GameService
    public GameService.GiftResult openGift (PlayerRecord player, int thingId, long created)
        throws ServiceException
    {
        final CardRecord card = _gameRepo.loadCard(-player.userId, thingId, created);
        if (card == null) {
            throw new ServiceException(GameService.E_UNKNOWN_CARD);
        }
        Thing thing = _thingRepo.loadThing(card.thingId);

        GameService.GiftResult result = new GameService.GiftResult();
        result.message = _gameRepo.loadGiftMessage(card); // must happen before prepareCard()
        prepareCard(player, thing, result, new Callable<CardRecord>() {
            public CardRecord call () throws Exception {
                _gameRepo.unwrapGift(card); // unwrap the card, adding it to our collection
                return card;
            }
        });

        // record a notice in their feed
        if (card.giverId == Card.BIRTHDAY_GIVER_ID) {
            _playerRepo.recordFeedItem(player.userId, FeedItem.Type.BIRTHDAY, 0, thing.name);
        } else {
            _playerRepo.recordFeedItem(
                player.userId, FeedItem.Type.GOTGIFT, card.giverId, thing.name);
        }

        return result;
    }

    public GameService.CardResult getAttractor (PlayerRecord player, int thingId, int friendId)
        throws ServiceException
    {
        long now = System.currentTimeMillis();

        boolean isOldAttractor;
        if (OLD_ATTRACTORS.contains(thingId)) {
            isOldAttractor = true;

        } else {
            Timestamp stamp = _gameRepo.getAttractorPostTime(friendId, thingId);
            if (stamp == null) {
                throw ServiceException.internalError(); // TODO: better error?
            }
            // it's old if it's more than 2 days old
            isOldAttractor = stamp.getTime() < (now - (GameUtil.ONE_DAY * 2));
        }

        // old attractors are only valid for brand-new (first 2 days) players
        if (isOldAttractor && (player.joined.getTime() < (now - (GameUtil.ONE_DAY * 2)))) {
            throw new ServiceException("This card is only free for new players. Sorry!");
        }

        // get information on the thing
        Thing thing = _thingRepo.loadThing(thingId);

        // see if the player already has one
        List<CardRecord> cards = _gameRepo.loadCards(player.userId, Ints.asList(thingId), false);
        if (!cards.isEmpty()) {
            // TODO: xlate
            throw new ServiceException("You already have the " + thing.name + " card!");
        }

        // see if we've ever granted them the attractor before...
        if (!_gameRepo.grantAttractor(player.userId, thingId)) {
            // TODO: xlate
            throw new ServiceException("You have already been granted a free " +
                thing.name + " card, you can't have another!");
        }

        // else, go ahead and grant them the card. Even if they're an old player.
        GameService.CardResult result = new GameService.CardResult();
        /*CardRecord card =*/ prepareCard(player, thing, result,
            cardCreator(player.userId, thingId, friendId));
        // TODO?
        return result;
    }

    protected CardRecord requireCard (int ownerId, int thingId, long received)
        throws ServiceException
    {
        CardRecord card = _gameRepo.loadCard(ownerId, thingId, received);
        if (card == null) {
            throw new ServiceException(GameService.E_UNKNOWN_CARD);
        }
        return card;
    }

    protected void checkCanPayForFlip (PlayerRecord player, int flipCost, int expectedCost)
        throws ServiceException
    {
        if (player.freeFlips >= 1) {
            return;
        }
        if (expectedCost != flipCost) {
            throw new ServiceException(GameService.E_FLIP_COST_CHANGED);
        }
        if (player.coins < flipCost) {
            throw new ServiceException(GameService.E_NSF_FOR_FLIP);
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
                throw new ServiceException(GameService.E_LACK_FREE_FLIP);
            }
        }

        // re-check that the expected cost matches
        if (expectedCost != flipCost) {
            throw new ServiceException(GameService.E_FLIP_COST_CHANGED);
        }

        // deduct the coins from the player's account
        if (!_playerRepo.consumeCoins(player.userId, expectedCost)) {
            throw new ServiceException(GameService.E_NSF_FOR_FLIP);
        }
    }

    protected CardRecord prepareCard (PlayerRecord player, Thing thing,
                                      GameService.CardResult result, Callable<CardRecord> creator)
    {
        // TODO: get this info from the ThingIndex, optimize this whole process
        SortedSet<Thing> things = Sets.newTreeSet(_thingRepo.loadThings(thing.categoryId));

        // load up the count of each card in this series held by the player
        Multiset<Integer> holdings = HashMultiset.create();
        for (CardRecord crec : _gameRepo.loadCards(
                 player.userId, Sets.newHashSet(Iterables.transform(things, EFuncs.THING_ID)),
                 false /* don't use the cache */)) {
            holdings.add(crec.thingId);
        }

        // include the number of cards we already have with this thing
        result.haveCount = holdings.count(thing.thingId);

        // now note this holding in our mapping and determine how many things remain
        holdings.add(thing.thingId);
        result.thingsRemaining = things.size() - holdings.elementSet().size();

        CardRecord card;
        try {
            // create the card (which adds it to the player's collection), then resolve it
            card = creator.call();
            result.card = _gameLogic.resolveCard(card, thing, things);
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // note that this player completed this series and if appropriate report to their feed
        if (result.thingsRemaining == 0 && result.haveCount == 0) {
            String how = result.getClass().getSimpleName();
            _gameLogic.maybeReportCompleted(player, result.card.getSeries(), how);

            result.trophies = _gameLogic.getNewTrophies(player.userId, thing.categoryId);
            // make sure each returned trophy has never been earned before...
            if (result.trophies != null) {
                for (Iterator<TrophyData> it = result.trophies.iterator(); it.hasNext(); ) {
                    if (!_gameLogic.maybeReportTrophy(player, it.next(), how)) {
                        it.remove();
                    }
                }
            }
        }

        return card;
    }

    protected Callable<CardRecord> cardCreator (
        final int userId, final int thingId, final int friendId)
    {
        return new Callable<CardRecord>() {
            public CardRecord call () throws Exception {
                return _gameRepo.createCard(userId, thingId, friendId);
            }
        };
    }

    /**
     * Return true if the specified card should be shown as a duplicate bonanza card.
     */
    protected boolean maybePickBonanza (PlayerRecord player, Card card)
    {
        // must be a I or II rarity
        if ((card.thing.rarity != Rarity.I) && (card.thing.rarity != Rarity.II)) {
            return false;
        }
        // see if they're eligible to be bothered
        if (!player.eligibleForAttractor() || (Math.random() >= BONANZA_CHANCE)) {
            return false;
        }
        // make sure they haven't posted this same attractor before
        if (null != _gameRepo.getAttractorPostTime(player.userId, card.thing.thingId)) {
            return false;
        }
        // make sure they don't hate this series
        if (Boolean.FALSE.equals(_playerRepo.getLike(player.userId, card.thing.categoryId))) {
            return false;
        }
        // it's good!
        return true;
    }

    @Inject protected GameLogic _gameLogic;
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected GameRepository _gameRepo;
    @Inject protected ThingRepository _thingRepo;

    /** The percent chance that a free flip will find a bonanza card. */
    protected static final double BONANZA_CHANCE = 0.2; // 20%

    /** Old attractors that are no longer in the DB but which we still allow to be received. */
    protected static final Set<Integer> OLD_ATTRACTORS = ImmutableSet.of(
        2119, // Voldemort
        767, // Beer
        232, // Little Penguin
        1170, // Chocolate cake
        1928, // maguro nigiri
        233, // siamese cat
        1801, // thin mints
        881, // wombat
        729, // Abbey Road
        1522, // breakdancing
        463, // miss piggy
        462, // kermit the frog
        2120, // sarumoan
        962); // statue of zeuss
}
