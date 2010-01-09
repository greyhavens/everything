//
// $Id$

package com.threerings.everything.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.IntSets;

import com.threerings.everything.client.GameService;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Grid;
import com.threerings.everything.data.GridStatus;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.Powerup;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.SlotStatus;
import com.threerings.everything.data.Thing;
import com.threerings.everything.data.ThingCard;
import com.threerings.samsara.app.client.ServiceException;

import com.threerings.everything.server.persist.CardRecord;
import com.threerings.everything.server.persist.GameRepository;
import com.threerings.everything.server.persist.GridRecord;
import com.threerings.everything.server.persist.LikeRecord;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;
import com.threerings.everything.server.persist.SlotStatusRecord;
import com.threerings.everything.server.persist.ThingRepository;

import static com.threerings.everything.Log.log;

/**
 * Provides game logics.
 */
@Singleton
public class GameLogic
{
    /**
     * Computes this player's game status given a freshly loaded player and grid record.
     */
    public GameStatus getGameStatus (PlayerRecord player, int[] unflipped)
    {
        GameStatus status = PlayerRecord.TO_GAME_STATUS.apply(player);
        if (status.freeFlips == 0) {
            status.nextFlipCost = getNextFlipCost(unflipped);
        }
        return status;
    }

    /**
     * Generates a new grid for the supplied player.
     */
    public GridRecord generateGrid (PlayerRecord player, Powerup pup, GridRecord previous)
        throws ServiceException
    {
        IntMap<Float> preferences = IntMaps.newHashIntMap();
        for (LikeRecord likeRec : _playerRepo.loadLikes(player.userId)) {
            preferences.put(likeRec.categoryId, likeRec.like ? LIKE_WEIGHT : DISLIKE_WEIGHT);
        }

        GridRecord grid = new GridRecord();
        grid.userId = player.userId;
        grid.gridId = (previous == null) ? 1 : previous.gridId + 1;
        grid.status = GridStatus.NORMAL;
        grid.thingIds = selectGridThings(player, pup, preferences);
        grid.expires = player.calculateNextExpires();

        // now that we successfully selected things for our grid, consume the powerup
        if (pup != Powerup.NOOP && !_gameRepo.consumePowerupCharge(player.userId, pup)) {
            throw new ServiceException(GameService.E_LACK_CHARGE);
        }

        return grid;
    }

    /**
     * Converts a grid record to a grid object, resolving all flipped cards into populated {@link
     * ThingCard} instances.
     */
    public Grid resolveGrid (GridRecord record)
    {
        Grid grid = GridRecord.TO_GRID.apply(record);

        // load up all the cards in the grid
        IntMap<Thing> things = IntMaps.newHashIntMap();
        for (Thing thing : _thingRepo.loadThings(IntSets.create(record.thingIds))) {
            things.put(thing.thingId, thing);
        }

        // load up the flipped status for the player's current grid
        SlotStatusRecord slots = _gameRepo.loadSlotStatus(record.userId);
        grid.slots = slots.toStatuses();
        long[] stamps = slots.toStamps();

        // if our grid status is non-normal, we need to resolve category data
        IntMap<String> reveal = null;
        switch (grid.status) {
        case CAT_REVEALED: reveal = resolveReveal(things.values(), 2); break;
        case SUBCAT_REVEALED: reveal = resolveReveal(things.values(), 1); break;
        case SERIES_REVEALED: reveal = resolveReveal(things.values(), 0); break;
        }

        // place the flipped cards into the runtime grid and summarize the rarities of the rest
        for (int ii = 0; ii < record.thingIds.length; ii++) {
            Thing thing = things.get(record.thingIds[ii]);
            if (thing == null) {
                log.warning("Missing thing for grid?", "userId", record.userId,
                            "gridId", record.gridId, "thingId", record.thingIds[ii]);
                continue;
            }
            switch (grid.slots[ii]) {
            case FLIPPED:
                grid.flipped[ii] = thing.toCard(stamps[ii]);
                break;
            case UNFLIPPED:
                grid.unflipped[thing.rarity.toByte()]++;
                if (reveal != null) {
                    grid.flipped[ii] = ThingCard.newPartial(reveal.get(thing.thingId));
                }
                break;
            default:
                break; // we do nada for GIFTED and SOLD
            }
        }

        return grid;
    }

    public Card resolveCard (CardRecord record)
    {
        return fillCard(resolveCard(record.thingId), record);
    }

    public Card resolveCard (int thingId)
    {
        Thing thing = _thingRepo.loadThing(thingId);
        SortedSet<Thing> things = Sets.newTreeSet(_thingRepo.loadThings(thing.categoryId));
        return resolveCard(thing, things);
    }

    /**
     * Resolves runtime card data for the supplied card.
     */
    public Card resolveCard (CardRecord record, Thing thing, SortedSet<Thing> things)
    {
        return fillCard(resolveCard(thing, things), record);
    }

    /**
     * Returns the cost of the next flip for the specified unflipped set.
     */
    public int getNextFlipCost (int[] unflipped)
    {
        // the cost of a flip is the expected value of the card
        int total = 0, count = 0;
        for (Rarity rarity : Rarity.values()) {
            int cards = unflipped[rarity.ordinal()];
            total += cards * rarity.value;
            count += cards;
        }
        return (count == 0) ? 0 : (total / count);
    }

    /**
     * Resolves the categories from the specified leaf category up.
     */
    public Category[] resolveCategories (int categoryId)
    {
        List<Category> cats = Lists.newArrayList();
        while (categoryId != 0) {
            Category cat = _thingRepo.loadCategory(categoryId);
            if (cat == null) {
                log.warning("Missing category in chain",  "cats", cats, "categoryId", categoryId);
                categoryId = 0;
            } else {
                cats.add(cat);
                categoryId = cat.parentId;
            }
        }
        Collections.reverse(cats);
        return cats.toArray(new Category[cats.size()]);
    }

    /**
     * Transfers the specified card to the specified target player. Takes care of feed message
     * generation and set completion checking and all the whatnots.
     */
    public void giftCard (
        PlayerRecord owner, CardRecord card, PlayerRecord target, String message,
        boolean recruitGift)
    {
        // transfer the card to the target player (in gift form)
        _gameRepo.giftCard(card, target.userId, message);

        // send a Facebook notification to the recipient
        Category series = _thingRepo.loadCategory(_thingRepo.loadThing(card.thingId).categoryId);
        _playerLogic.sendGiftNotification(owner, target.facebookId, series);

        if (!recruitGift) {
            // it may be on their grid, in which case we need to update its status
            noteCardStatus(card, SlotStatus.GIFTED);
        }
    }

    /**
     * Records and reports that the specified player completed the specified series if they haven't
     * already completed the series.
     *
     * @return true if we reported the series completion, false if not.
     */
    public boolean maybeReportCompleted (PlayerRecord user, Category series, String how)
    {
        if (_gameRepo.noteCompletedSeries(user.userId, series.categoryId)) {
            log.info("Series completed!", "who", user.who(), "series", series.name, "how", how);
            _playerRepo.recordFeedItem(user.userId, FeedItem.Type.COMPLETED, 0, series.name);
            return true;
        }
        return false;
    }

    /**
     * Potentially updates a player's grid when they sell or gift a card. If the card is on their
     * current grid, the slot for the card is updated.
     */
    public void noteCardStatus (CardRecord card, SlotStatus status)
    {
        // TODO: this is a little hacky. It's possible that they're trying to give away their
        // old card but we're zapping its twin from their grid. Their collection will be
        // as intended, but the grid won't look right.
        if (card.giverId == 0 && (System.currentTimeMillis() - card.received.getTime()) < 24*HOUR) {
            GridRecord grid = _gameRepo.loadGrid(card.ownerId);
            if (grid == null) {
                return;
            }
            for (int ii = 0; ii < grid.thingIds.length; ii++) {
                if (grid.thingIds[ii] == card.thingId) {
                    _gameRepo.updateSlot(card.ownerId, ii, SlotStatus.FLIPPED, status);
                    return;
                }
            }
        }
    }

    /**
     * Called every hour to grant a gift to anyone who's birthday has arrived and has not yet
     * received a gift.
     */
    public void processBirthdays ()
    {
        // TODO: when we need to scale have one server load up the ids of the birthday players,
        // divide it up and farm it out to all the servers for gift selection and granting
        for (PlayerRecord player : _playerRepo.loadBirthdayPlayers()) {
            try {
                processBirthday(player);
            } catch (Exception e) {
                log.warning("Failure processing player's birthday", "who", player.who(), e);
            }
        }
    }

    /**
     * Selects the things that will be contained in a fresh grid for the specified player. The
     * things will be selected using our most recently loaded snapshot of the thing database based
     * on the aggregate rarities of all of the things in that snapshot.
     */
    public int[] selectGridThings (PlayerRecord player, Powerup pup, IntMap<Float> preferences)
        throws ServiceException
    {
        ThingIndex index = _thingLogic.getThingIndex().copyWeighted(preferences);
        IntSet thingIds = IntSets.create();

        // load up this player's collection summary, identify incomplete series
        Multimap<Integer, Integer> collection = _gameRepo.loadCollection(player.userId, index);
        IntSet haveIds = IntSets.create(collection.values());
        IntSet haveCats = IntSets.create();
        for (int categoryId : collection.keySet()) {
            if (index.getCategorySize(categoryId) > collection.get(categoryId).size()) {
                haveCats.add(categoryId);
            }
        }

        // determine which cards this player needs to complete their collections
        IntSet needed = index.computeNeeded(collection);

        // we want to give them one card of high rarity that they need, if they used a powerup that
        // guarantees a card of a particular rarity, attempt to satisfy that with a needed card
        if (pup.getBonusRarity() != null) {
            index.pickThingOf(pup.getBonusRarity(), needed, thingIds);
        } else {
            // if we're not using a rarity powerup, pick a bonus needed card for the grid
            index.pickBonusThing(needed, thingIds);
        }
        boolean gotNeeded = (thingIds.size() > 0) && needed.contains(thingIds.iterator().next());
        log.info("Selected rare thing", "for", player.who(), "rarity", pup.getBonusRarity(),
                 "needed", needed.size(), "gotNeeded", gotNeeded, "ids", thingIds);

        // now ensure that they have 25% cards they need, (one may have been chosen above)
        int neededGimmes = Grid.GRID_SIZE/4 - (gotNeeded ? 1 : 0);
        neededGimmes = Math.min(haveCats.size(), neededGimmes); // adjust for small collections
        if (neededGimmes > 0) {
            index.selectThingsFrom(haveCats, neededGimmes, haveIds, thingIds);
            log.info("Selected needed cards", "for", player.who(), "count", neededGimmes,
                     "ids", thingIds);
        }

        // select up to half of our cards from series we are collecting
        int fromCollected;
        switch (pup) {
        case ALL_COLLECTED_SERIES:
            if (haveCats.size() < Grid.GRID_SIZE/2) {
                throw new ServiceException(GameService.E_TOO_FEW_SERIES);
            }
            fromCollected = Grid.GRID_SIZE - thingIds.size();
            break;
        case ALL_NEW_CARDS:
            fromCollected = 0;
            break;
        default:
            fromCollected = Math.min(haveCats.size(), Grid.GRID_SIZE/2) - neededGimmes;
            break;
        }
        if (fromCollected > 0) {
            index.selectThingsFrom(haveCats, fromCollected, thingIds);
            log.info("Selected cards from collection", "for", player.who(),
                     "haveCats", haveCats.size(), "count", fromCollected, "ids", thingIds);
        }

        // if they requested all new cards, load up the things they own
        IntSet excludeIds = (pup == Powerup.ALL_NEW_CARDS) ? haveIds : IntSets.create();

        // now select the remainder randomly from all possible things
        int randoCount = Grid.GRID_SIZE - thingIds.size();
        if (randoCount > 0) {
            index.selectThings(randoCount, excludeIds, thingIds);
            log.info("Selected random cards", "for", player.who(), "count", randoCount,
                     "excluded", excludeIds.size(), "ids", thingIds);
        }

        // sanity check that we don't have too many things
        if (thingIds.size() > Grid.GRID_SIZE) {
            log.warning("Zoiks! Generated too many things for grid", "who", player.who(),
                        "pup", pup, "fromCollected", fromCollected, "randoCount", randoCount,
                        "excludes", excludeIds.size());
            while (thingIds.size() > Grid.GRID_SIZE) {
                thingIds.remove(thingIds.interator().nextInt());
            }
        }

        // shuffle the resulting thing ids for maximum randosity
        int[] ids = thingIds.toIntArray();
        ArrayUtil.shuffle(ids);
        return ids;
    }

    /**
     * Returns a set of categories the player is collecting (ie. they have at least one card in the
     * category but have not completed it).
     */
    protected IntSet resolveOwnedCats (int userId, ThingIndex index)
    {
        IntIntMap owned = _thingRepo.loadPlayerSeriesInfo(userId);
        IntSet ownedCats = IntSets.create();
        for (IntIntMap.IntIntEntry entry : owned.entrySet()) {
            if (entry.getIntValue() < index.getCategorySize(entry.getIntKey())) {
                ownedCats.add(entry.getIntKey());
            }
        }
        return ownedCats;
    }

    /**
     * Resolves either nothing, the series, the sub-category or the category for the specified
     * collection of things. Returns a map from thing id to the resolved name.
     */
    protected IntMap<String> resolveReveal (Collection<Thing> things, int reductions)
    {
        // first create a mapping from thing to category
        IntMap<Category> cats = loadCategoryMap(
            Sets.newHashSet(Iterables.transform(things, EFuncs.CATEGORY_ID)));
        Map<Integer, Category> thingcat = Maps.newHashMap();
        for (Thing thing : things) {
            thingcat.put(thing.thingId, cats.get(thing.categoryId));
        }

        // now reduce the specified number of times
        while (reductions > 0) {
            cats = loadCategoryMap(
                Sets.newHashSet(Iterables.transform(thingcat.values(), EFuncs.PARENT_ID)));
            for (Map.Entry<Integer, Category> entry : thingcat.entrySet()) {
                entry.setValue(cats.get(entry.getValue().parentId));
            }
            reductions--;
        }

        // finally extract the names of these categories
        IntMap<String> reveals = IntMaps.newHashIntMap();
        for (Map.Entry<Integer, Category> entry : thingcat.entrySet()) {
            reveals.put(entry.getKey(), entry.getValue().name);
        }
        return reveals;
    }

    protected IntMap<Category> loadCategoryMap (Set<Integer> catIds)
    {
        IntMap<Category> cats = IntMaps.newHashIntMap();
        for (Category cat : _thingRepo.loadCategories(catIds)) {
            cats.put(cat.categoryId, cat);
        }
        return cats;
    }

    /**
     * Selects and gifts a card to the specified player for their birthday.
     */
    protected void processBirthday (PlayerRecord user)
    {
        ThingIndex index = _thingLogic.getThingIndex();
        IntSet heldRares = _thingRepo.loadPlayerThings(user.userId, Rarity.MIN_GIFT_RARITY);
        int thingId = index.pickBirthdayThing(resolveOwnedCats(user.userId, index), heldRares);
        Thing thing = _thingRepo.loadThing(thingId);

        // grant this card to the player (in "wrapped" state)
        _gameRepo.createCard(-user.userId, thingId, Card.BIRTHDAY_GIVER_ID);

        log.info("Gave out a birthday present", "to", user.who(), "what", thing.name);
    }

    /**
     * Create a Card from the specified Thing, filling in the positional information from the
     * set of things in the series.
     */
    protected Card resolveCard (Thing thing, SortedSet<Thing> things)
    {
        Card card = new Card();
        card.thing = thing;
        card.categories = resolveCategories(thing.categoryId);
        // we have to load all the things in this category for this bit
        // (TODO: get series size and card position from the ThingIndex)
        card.things = things.size();
        for (Thing tcard : things) {
            if (card.thing.thingId == tcard.thingId) {
                break;
            }
            card.position++;
        }
        return card;
    }

    /**
     * Fill in a created Card instance with the information from the CardRecord and database.
     */
    protected Card fillCard (Card card, CardRecord record)
    {
        card.owner = _playerRepo.loadPlayerName(record.ownerId);
        card.received = new Date(record.received.getTime());
        if (record.giverId > 0) {
            card.giver = _playerRepo.loadPlayerName(record.giverId);
        } else if (record.giverId == Card.BIRTHDAY_GIVER_ID) {
            card.giver = PlayerName.create(Card.BIRTHDAY_GIVER_ID);
        }
        return card;
    }

    @Inject protected EverythingApp _app;
    @Inject protected GameRepository _gameRepo;
    @Inject protected KontagentLogic _kontLogic;
    @Inject protected PlayerLogic _playerLogic;
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected ThingLogic _thingLogic;
    @Inject protected ThingRepository _thingRepo;

    protected static final Float LIKE_WEIGHT = 2f;

    protected static final Float DISLIKE_WEIGHT = .5f;

    /** We'll try 10 times to pick a bonus card before giving up. */
    protected static final int MAX_BONUS_ATTEMPTS = 10;

    /** One hour in milliseconds. */
    protected static final long HOUR = 60*60*1000L;
}
