//
// $Id$

package com.threerings.everything.server;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Callable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Tuple;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;

import com.threerings.everything.client.GameService;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.FriendCardInfo;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Grid;
import com.threerings.everything.data.GridStatus;
import com.threerings.everything.data.Player;
import com.threerings.everything.data.PlayerCollection;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.Powerup;
import com.threerings.everything.data.Series;
import com.threerings.everything.data.SeriesCard;
import com.threerings.everything.data.SlotStatus;
import com.threerings.everything.data.Thing;
import com.threerings.everything.data.ThingCard;
import com.threerings.everything.server.persist.CardRecord;
import com.threerings.everything.server.persist.GameRepository;
import com.threerings.everything.server.persist.GridRecord;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.ThingRepository;
import com.threerings.everything.util.GameUtil;

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
        PlayerCollection coll = new PlayerCollection();
        coll.owner = _playerRepo.loadPlayerName(ownerId);
        if (coll.owner == null) {
            throw new ServiceException(E_UNKNOWN_USER);
        }

        // first load up all of the series
        Multimap<Integer, SeriesCard> series = HashMultimap.create();
        // TODO: use loadPlayerThings and resolve category data from memory
        for (SeriesCard card : _thingRepo.loadPlayerSeries(ownerId)) {
            series.put(card.parentId, card);
        }

        // load up the series' parents, the sub-categories
        Multimap<Integer, Category> subcats = HashMultimap.create();
        for (Category subcat : _thingRepo.loadCategories(series.keySet())) {
            subcats.put(subcat.parentId, subcat);
        }

        // finally load up the top-level categories and build everything back down
        coll.series = Maps.newTreeMap();
        for (Category cat : _thingRepo.loadCategories(subcats.keySet())) {
            Map<String, List<SeriesCard>> scats = Maps.newTreeMap();
            for (Category scat : subcats.get(cat.categoryId)) {
                List<SeriesCard> slist = Lists.newArrayList(series.get(scat.categoryId));
                Collections.sort(slist);
                scats.put(scat.name, slist);
            }
            coll.series.put(cat.name, scats);
        }

        return coll;
    }

    // from interface GameService
    public Series getSeries (int ownerId, int categoryId) throws ServiceException
    {
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
                    card.received = crec.received.getTime();
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
        return _gameLogic.resolveCard(requireCard(ident.ownerId, ident.thingId, ident.received));
    }

    // from interface GameService
    public GridResult getGrid (Powerup pup, boolean expectHave) throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        long now = System.currentTimeMillis();
        GridRecord grid = _gameRepo.loadGrid(player.userId);
        if (grid == null || grid.expires.getTime() < now) {
            if (expectHave) {
                return null; // they thought they had a grid, but they don't, let them know
            }

            // note the time that their old grid expired
            long oexpires = (grid == null) ? (now - GameUtil.ONE_DAY) : grid.expires.getTime();

            // generate a new grid
            grid = _gameLogic.generateGrid(player, pup, grid);

            // store the new grid in ze database and reset the player's flipped status
            _gameRepo.storeGrid(grid);
            _gameRepo.resetSlotStatus(player.userId);

            // based on the time that has elapsed between grid expirations, grant them free flips
            long elapsed = grid.expires.getTime() - oexpires;
            int grantFlips = GameUtil.computeFreeFlips(player, elapsed);
            _playerRepo.grantFreeFlips(player, grantFlips);

            log.info("Generated grid", "for", player.who(), "pup", pup, "things", grid.thingIds,
                     "expires", grid.expires, "elapsed", elapsed, "flips", grantFlips);
        }

        GridResult result = new GridResult();
        result.grid = _gameLogic.resolveGrid(grid);
        result.status = _gameLogic.getGameStatus(player, result.grid.unflipped);
        log.info("Returning grid", "for", player.who(), "things", grid.thingIds,
                 "unflipped", result.grid.unflipped);
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
        if (!_gameRepo.flipSlot(player.userId, position)) {
            throw new ServiceException(E_ALREADY_FLIPPED);
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
        final Thing thing = _thingRepo.loadThing(grec.thingIds[position]);

        log.info("Yay! Card flipped", "who", player.who(), "thing", thing.name,
                 "rarity", thing.rarity, "paid", expectedCost);

        // create the card, add it to their collection and resolve associated bits
        final int userId = player.userId;
        FlipResult result = new FlipResult();
        CardRecord card = prepareCard(player, thing, result, new Callable<CardRecord>() {
            public CardRecord call () throws Exception {
                return _gameRepo.createCard(userId, thing.thingId, 0);
            }
        });

        // decrement the unflipped count for the flipped card's rarity so that we can properly
        // compute the new next flip cost
        grid.unflipped[thing.rarity.ordinal()]--;
        result.status = _gameLogic.getGameStatus(player, grid.unflipped);

        // note the received timestamp of the created card in this position
        _gameRepo.updateSlot(player.userId, position, card.received.getTime());

        // record that this player flipped this card
        _playerRepo.recordFeedItem(player.userId, FeedItem.Type.FLIPPED, 0, thing.name);

        // on a free flip, there's a chance they'll get a bonanza card
        if (expectedCost == 0) {
            result.bonanza = maybePickBonanza(player);
        }

        return result;
    }

    // from interface GameService
    public int sellCard (int thingId, long received) throws ServiceException
    {
        PlayerRecord player = requirePlayer();
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
        return player.coins + coins;
    }

    // from interface GameService
    public GiftInfoResult getGiftCardInfo (int thingId, long received) throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        requireCard(player.userId, thingId, received);

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
            // some of their friends may be samsara users but not everything players: skip nulls
            info.friend = names.get(friendId);
            if (info.friend != null) {
                info.hasThings = holdings.getOrElse(friendId, 0);
                info.onWishlist = false; // TODO
                result.friends.add(info);
            }
        }
        return result;
    }

    // from interface GameService
    public void giftCard (int thingId, long received, int friendId, String message)
        throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        PlayerRecord friend = _playerRepo.loadPlayer(friendId);
        if (friend == null) {
            log.warning("Requested to gift to unknown friend?", "gifter", player.who(),
                        "friendId", friendId);
            throw new ServiceException(AppCodes.E_INTERNAL_ERROR);
        }
        CardRecord card = requireCard(player.userId, thingId, received);
        _gameLogic.giftCard(player, card, friend, message, false);
    }

    // from interface GameService
    public GameStatus bonanzaViewed (boolean posted)
        throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        if (!player.eligibleForAttractor()) {
            throw new ServiceException(AppCodes.E_INTERNAL_ERROR); // TODO: better error?
        }
        // if they posted the attractor, give them another one 2 days from now, otherwise 5 days..
        _playerRepo.setNextAttractor(player,
            new Timestamp(player.calculateNextExpires().getTime() +
                ((posted ? 1 : 4) * GameUtil.ONE_DAY)));
        if (!posted) {
            return null; // that's it, they turned us down, screw them.
        }
        // otherwise, grant them a free flip
        _playerRepo.grantFreeFlips(player, 1);
        // return their game status
        return _gameLogic.getGameStatus(player, null /* unflipped */);
        // (Note: we pass null for 'unflipped' because it won't be used since we know for a fact
        // that they have at least one free flip (we just granted it!))
    }

    // from interface GameService
    public GiftResult openGift (int thingId, long created)
        throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        final CardRecord card = _gameRepo.loadCard(-player.userId, thingId, created);
        if (card == null) {
            throw new ServiceException(E_UNKNOWN_CARD);
        }
        Thing thing = _thingRepo.loadThing(card.thingId);

        GiftResult result = new GiftResult();
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

    // from interface GameService
    public ShopResult getShopInfo () throws ServiceException
    {
        PlayerRecord player = requirePlayer();
        ShopResult result = new ShopResult();
        result.coins = player.coins;
        result.powerups = _gameRepo.loadPowerups(player.userId);
        return result;
    }

    // from interface GameService
    public void buyPowerup (Powerup type) throws ServiceException
    {
        PlayerRecord player = requirePlayer();

        // sanity check
        if (type == null || type.cost <= 0) {
            throw new ServiceException(AppCodes.E_INTERNAL_ERROR);
        }

        // if this is a permanent powerup, make sure they don't already own it
        if (type.isPermanent()) {
            if (_gameRepo.loadPowerupCount(player.userId, type) > 0) {
                throw new ServiceException(E_ALREADY_OWN_POWERUP);
            }
        }

        // if this is a flag granting powerup, make sure they don't already have the flag set (this
        // should be caught by the previous check but we'll be extra sure)
        Player.Flag flag = type.getTargetFlag();
        if (flag != null && player.isSet(flag)) {
            throw new ServiceException(E_ALREADY_OWN_POWERUP);
        }

        // deduct the coins from the player's account
        if (!_playerRepo.consumeCoins(player.userId, type.cost)) {
            throw new ServiceException(E_NSF_FOR_PURCHASE);
        }

        // grant them the powerup charges
        _gameRepo.grantPowerupCharges(player.userId, type, type.charges);

        // if this was a flag granting powerup, activate the flag
        if (flag != null) {
            _playerRepo.updateFlag(player, flag, true);
        }
    }

    // from interface GameService
    public Grid usePowerup (int gridId, Powerup type) throws ServiceException
    {
        PlayerRecord player = requirePlayer();

        // make sure we're all talking about the same grid
        GridRecord grid = _gameRepo.loadGrid(player.userId);
        if (grid == null || grid.expires.getTime() < System.currentTimeMillis() ||
            grid.gridId != gridId) {
            throw new ServiceException(E_GRID_EXPIRED);
        }

        // determine the grid's new status
        GridStatus status = type.getTargetStatus();
        if (status == null) {
            log.warning("Requested to apply invalid powerup to grid", "who", player.who(),
                        "powerup", type);
            throw new ServiceException(AppCodes.E_INTERNAL_ERROR);
        }

        // save the player from wasting a charge (the client prevents this as well)
        if (grid.status == status) {
            log.warning("Preventing NOOP powerup usage", "who", player.who(), "powerup", type);
            throw new ServiceException(AppCodes.E_INTERNAL_ERROR);
        }

        // consume the powerup
        if (!_gameRepo.consumePowerupCharge(player.userId, type)) {
            throw new ServiceException(E_LACK_CHARGE);
        }

        // update the grid's status in the database (and in memory)
        _gameRepo.updateGridStatus(grid, status);

        // re-resolve and return the grid
        return _gameLogic.resolveGrid(grid);
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

    protected CardRecord prepareCard (PlayerRecord player, Thing thing, CardResult result,
                                      Callable<CardRecord> creator)
    {
        // TODO: get this info from the ThingIndex, optimize this whole process
        SortedSet<Thing> things = Sets.newTreeSet(_thingRepo.loadThings(thing.categoryId));

        // load up the count of each card in this series held by the player
        IntIntMap holdings = new IntIntMap();
        for (CardRecord crec : _gameRepo.loadCards(
                 player.userId, Sets.newHashSet(Iterables.transform(things, EFuncs.THING_ID)),
                 false /* don't use the cache */)) {
            holdings.increment(crec.thingId, 1);
        }

        // include the number of cards we already have with this thing
        result.haveCount = holdings.getOrElse(thing.thingId, 0);

        // now note this holding in our mapping and determine how many things remain
        holdings.increment(thing.thingId, 1);
        result.thingsRemaining = things.size() - holdings.size();

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
        if (result.thingsRemaining == 0) {
            _gameLogic.maybeReportCompleted(player, result.card.getSeries(),
                                            result.getClass().getSimpleName());
        }

        return card;
    }

    protected CardRecord requireCard (int ownerId, int thingId, long received)
        throws ServiceException
    {
        CardRecord card = _gameRepo.loadCard(ownerId, thingId, received);
        if (card == null) {
            throw new ServiceException(E_UNKNOWN_CARD);
        }
        return card;
    }

    /**
     * Maybe pick an attractor card to return as a "bonanza card".
     */
    protected BonanzaInfo maybePickBonanza (PlayerRecord player)
    {
        if (!player.eligibleForAttractor() || (Math.random() >= BONANZA_CHANCE)) {
            return null;
        }
        // OK! pick a card
        ThingIndex index = _thingLogic.getThingIndex();
        Tuple<Integer, Tuple<String, String>> att = index.pickAttractor();
        if (att == null) {
            return null; // nothing to pick!
        }

        BonanzaInfo info = new BonanzaInfo();
        info.card = _gameLogic.resolveCard(att.left);
        info.title = att.right.left;
        info.message = att.right.right;
        return info;
    }

    @Inject protected GameLogic _gameLogic;
    @Inject protected GameRepository _gameRepo;
    @Inject protected ThingLogic _thingLogic;
    @Inject protected ThingRepository _thingRepo;

    /** The percent chance that a free flip will find a bonanza card. */
    protected static final double BONANZA_CHANCE = 0.2; // 20%
}
