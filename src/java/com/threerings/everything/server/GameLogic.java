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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ArrayUtil;

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
import com.threerings.everything.data.SeriesCard;
import com.threerings.everything.data.SlotStatus;
import com.threerings.everything.data.Thing;
import com.threerings.everything.data.ThingCard;
import com.threerings.everything.data.TrophyData;
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
        Map<Integer, Float> weights = generateSeriesWeights(player.userId);

        GridRecord grid = new GridRecord();
        grid.userId = player.userId;
        grid.gridId = (previous == null) ? 1 : previous.gridId + 1;
        grid.status = GridStatus.NORMAL;
        grid.thingIds = selectGridThings(player, pup, weights);
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
        Map<Integer, Thing> things = Maps.newHashMap();
        for (Thing thing : _thingRepo.loadThings(Ints.asList(record.thingIds))) {
            things.put(thing.thingId, thing);
        }

        // load up the flipped status for the player's current grid
        SlotStatusRecord slots = _gameRepo.loadSlotStatus(record.userId);
        grid.slots = slots.toStatuses();
        long[] stamps = slots.toStamps();

        // if our grid status is non-normal, we need to resolve category data
        Map<Integer, String> reveal = null;
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
                log.warning("Missing category in chain",  "cats", cats, "categoryId", categoryId,
                    new Exception("PANIC?")); // TEMP so I can see where this is coming from
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
     * Records and reports that the specified player earned the specified trophy if they hadn't
     * already earned this trophy.
     *
     * @return true if we reported the trophy earning, false if not.
     */
    public boolean maybeReportTrophy (PlayerRecord user, TrophyData trophy, String how)
    {
        if (_gameRepo.noteTrophyEarned(user.userId, trophy.trophyId)) {
            log.info("Trophy earned!", "who", user.who(), "trophy", trophy.trophyId, "how", how);
            _playerRepo.recordFeedItem(user.userId, FeedItem.Type.TROPHY, 0, trophy.name);
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
     * Return any new trophies to be awarded this player based on their completion of the specified
     * series.
     *
     * @return null if there are no new trophies to report
     */
    public List<TrophyData> getNewTrophies (int userId, int completedSeriesId)
    {
        return getTrophies(_thingRepo.loadPlayerSeries(userId), completedSeriesId);
    }

    /**
     * Return all the trophies awarded this player, based purely on their series ownerships.
     *
     * @return null if there are no qualifying trophies.
     */
    public List<TrophyData> getTrophies (List<SeriesCard> seriesCards)
    {
        return getTrophies(seriesCards, 0);
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
    public int[] selectGridThings (PlayerRecord player, Powerup pup, Map<Integer, Float> weights)
        throws ServiceException
    {
        ThingIndex index = _thingLogic.getThingIndex();
        Set<Integer> thingIds = Sets.newHashSet();

        // load up this player's collection summary, identify incomplete series
        Multimap<Integer, Integer> collection = _gameRepo.loadCollection(player.userId, index);
        Set<Integer> haveIds = Sets.newHashSet(collection.values());
        Set<Integer> haveCats = Sets.newHashSet();
        for (Map.Entry<Integer, Collection<Integer>> entry : collection.asMap().entrySet()) {
            int categoryId = entry.getKey();
            int collCatCount = entry.getValue().size(); // player's collection size for this cat
            int categorySize = index.getCategorySize(categoryId);
            if (categorySize > collCatCount) {
                haveCats.add(categoryId);

            } else if (categorySize == collCatCount) { // only other possibility, actually...
                // The player has completed this collection. Nix any upwards weightings.
                Float weight = weights.get(categoryId);
                if (weight != null && weight >= 1f) {
                    weights.remove(categoryId);
                }
            }
        }
        // then, copy the index to one that's been weighted according to the user's preferences
        index = index.copyWeighted(weights);

        // determine which cards this player needs to complete their collections
        Set<Integer> needed = index.computeNeeded(collection);

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
        Set<Integer> excludeIds =
            (pup == Powerup.ALL_NEW_CARDS) ? haveIds : Collections.<Integer>emptySet();

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
                thingIds.remove(thingIds.iterator().next());
            }
        }

        // shuffle the resulting thing ids for maximum randosity
        int[] ids = Ints.toArray(thingIds);
        ArrayUtil.shuffle(ids);
        return ids;
    }

    /**
     * Get all the qualifying trophies.
     *
     * @param completedSeriesId if nonzero, a seriesId that must be present in any
     * trophy requirement.
     */
    protected List<TrophyData> getTrophies (List<SeriesCard> seriesCards, int completedSeriesId)
    {
        Set<Integer> completedSets = Sets.newHashSet();
        for (SeriesCard series : seriesCards) {
            if (series.owned == series.things) {
                completedSets.add(series.categoryId);
            }
        }

        List<TrophyData> result = null;
        for (TrophyRecord rec : _trophies) {
            if ((completedSeriesId == 0) || (rec.sets == null) ||
                    rec.sets.contains(completedSeriesId)) {
                int have = (rec.sets == null)
                    ? completedSets.size()
                    :Sets.intersection(completedSets, rec.sets).size();
                for (Map.Entry<Integer, TrophyData> entry : rec.trophies.entrySet()) {
                    if (have >= entry.getKey()) {
                        if (result == null) {
                            result = Lists.newArrayList();
                        }
                        result.add(entry.getValue());
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns a set of categories the player is collecting (ie. they have at least one card in the
     * category but have not completed it).
     */
    protected Set<Integer> resolveOwnedCats (int userId, final ThingIndex index)
    {
        Map<Integer, Integer> owned = _thingRepo.loadPlayerSeriesInfo(userId);
        Set<Integer> ownedCats = Sets.newHashSet();
        for (Map.Entry<Integer, Integer> entry : owned.entrySet()) {
            if (entry.getValue() < index.getCategorySize(entry.getKey())) {
                ownedCats.add(entry.getKey());
            }
        }
        return ownedCats;
    }

    /**
     * Generate weightings to apply to seriesIds for the specified user.
     */
    protected Map<Integer, Float> generateSeriesWeights (int userId)
    {
        // Build a map of the player's category weightings
        Map<Integer, Float> weights = Maps.newHashMap();
        for (LikeRecord likeRec : _playerRepo.loadLikes(userId)) {
            weights.put(likeRec.categoryId,
                likeRec.like ? ThingLogic.LIKE_WEIGHT : ThingLogic.DISLIKE_WEIGHT);
        }

        // load friend likings and decide how much they matter
        Collection<Integer> friendIds = _playerRepo.loadFriendIds(userId).toList();
        Map<Integer, Float> friendLikes = friendIds.isEmpty()
            ? Collections.<Integer, Float>emptyMap()
            : _playerRepo.loadCollectiveLikes(friendIds);
        // calculate a factor for our friends' weightings according to how many we have:
        // from .2 (1 friend) to .8 (9 or more friends). Don't worry about the 0 friend case.
        float friendFactor = .2f + (Math.min(8, friendIds.size() - 1) / 8f) * .6f;

        // go through global weights and factor them with friend weights...
        for (Map.Entry<Integer, Float> entry : _thingLogic.getGlobalLikes().entrySet()) {
            Integer catId = entry.getKey();
            if (!weights.containsKey(catId)) {
                // only for categories we don't already have personally weighted
                weights.put(catId, ThingLogic.LIKABILITY_TO_WEIGHT.apply(
                    combineLikability(entry.getValue(), friendLikes.get(catId), friendFactor)));
            }
        }
//        // Let's dump the weightings:
//        for (Map.Entry<Integer, Float> entry : weights.entrySet()) {
//            System.err.println("\t" + entry.getKey() + " => " + entry.getValue());
//        }
        return weights;
    }

    /**
     * Helper for generateSeriesWeights: combine global and friend likability.
     */
    protected Float combineLikability (Float globalLiking, Float friendLiking, float friendFactor)
    {
        return (friendLiking == null)
            ? globalLiking
            : ((friendLiking * friendFactor) + ((1 - friendFactor) * globalLiking));
    }

    /**
     * Resolves either nothing, the series, the sub-category or the category for the specified
     * collection of things. Returns a map from thing id to the resolved name.
     */
    protected Map<Integer, String> resolveReveal (Collection<Thing> things, int reductions)
    {
        // first create a mapping from thing to category
        Map<Integer, Category> cats = loadCategoryMap(
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
        Map<Integer, String> reveals = Maps.newHashMap();
        for (Map.Entry<Integer, Category> entry : thingcat.entrySet()) {
            reveals.put(entry.getKey(), entry.getValue().name);
        }
        return reveals;
    }

    protected Map<Integer, Category> loadCategoryMap (Set<Integer> catIds)
    {
        Map<Integer, Category> cats = Maps.newHashMap();
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
        Set<Integer> heldRares = _thingRepo.loadPlayerThings(user.userId, Rarity.MIN_GIFT_RARITY);
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

    public static class TrophyRecord
    {
        public final ImmutableSet<Integer> sets;

        public final ImmutableMap<Integer, TrophyData> trophies;

        /**
         * Construct a set of related trophies.
         *
         * Replacements: %n - number (of sets)
         *               %o - ordinal (position of trophy, starting with 1)
         *               %r - roman (oridnal in roman numerals)
         */
        protected TrophyRecord (
            ImmutableSet<Integer> sets, String trophyId, String name, String desc, int... sizes)
        {
            this.sets = sets;
            if (sizes.length == 0) {
                sizes = new int[] { sets.size() };
            }
            ImmutableMap.Builder<Integer, TrophyData> builder = ImmutableMap.builder();
            for (int ii = 0; ii < sizes.length; ii++) {
                builder.put(sizes[ii],
                    new TrophyData(
                        replace(trophyId, ii + 1, sizes[ii]),
                        replace(name, ii + 1, sizes[ii]),
                        replace(desc, ii + 1, sizes[ii])));
            }
            trophies = builder.build();
        }

        protected static String replace (String s, int ordinal, int number)
        {
            s = s.replace("%o", String.valueOf(ordinal));
            s = s.replace("%n", String.valueOf(number));
            s = s.replace("%r", toRoman(ordinal));
            return s;
        }

        protected static String toRoman (int number)
        {
            String roman = "";
            for (int ii = 0; ii < ROMAN_TIERS.length; ii++) {
                while (number >= ROMAN_TIERS[ii]) {
                    roman += ROMAN_NUMERALS[ii];
                    number -= ROMAN_TIERS[ii];
                }
            }
            return roman;
        }

        /** Standard roman numerals support up to 3999, but it is extremely unlikely that we ever
         * have more than 20 or so levels. */
        protected static final int[] ROMAN_TIERS = { 10, 9, 5, 4, 1 };
        protected static final String[] ROMAN_NUMERALS = { "X", "IX", "V", "IV", "I" };
//        /** Look ma, unicode characters for the precision. */
//        protected static final int[] ROMAN_TIERS = {
//            11, 10, 9, 8, 7, 6, 5, 4,
//            3, 2, 1 };
//        protected static final String[] ROMAN_NUMERALS = {
//            "\u216a", "\u2169", "\u2168", "\u2167", "\u2166", "\u2165", "\u2164", "\u2163",
//            "\u2162", "\u2161", "\u2160" };
    }

    /**
     * Trophy data. TODO: from database...
     */
    protected final List<TrophyRecord> _trophies = ImmutableList.of(
        // a series of trophies awarded purely for completing numbers of sets?
        new TrophyRecord(null,
            "sets%n", "Completed %n", "Complete %n sets of any kind",
            1, 3, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200, 250),
//        new TrophyRecord(null,
//            "test%n", "Test %r", "Test test %n sets",
//            0, 1, 2, 3),
        // simple trophies requiring complete collection
        new TrophyRecord(
            ImmutableSet.of(311, 315, 322, 332),
            "presidents", "U.S. Presidents", "Collect all U.S. Presidents"),
        new TrophyRecord(
            ImmutableSet.of(430, 432, 434),
            "carnivore", "Carnivore", "Collect all the cuts of meat"),
        new TrophyRecord(
            ImmutableSet.of(154, 155, 156, 157, 158, 159, 160),
            "consoles", "Game Consoles", "Collect every generation of game console"),
        new TrophyRecord(
            ImmutableSet.of(350, 351, 352, 353),
            "us_states", "All 50 States", "Collect every US State"),
        new TrophyRecord(
            ImmutableSet.of(486, 488, 489, 490, 493),
            "simpsons", "All Simpsons", "Collect every Simpsons character"),
        new TrophyRecord(
            ImmutableSet.of(465, 479, 480, 481, 482, 483),
            "start_trek", "All Star Trek", "Collect every Star Trek set"),
        new TrophyRecord(
            ImmutableSet.of(526, 527, 528),
            "herbs", "All Herbs and Spices", "Collect all the Herbs and Spices"),
        // more complex trophies requiring subsets of the sets
        new TrophyRecord(
            ImmutableSet.of(114, 184, 188, 189, 199, 205, 211, 273),
            "sevens", "Sevens", "Collect seven 'Seven' series",
            7), // need 7 of 8
        // standard "bla I", "bla II", "bla III" series collections
        new TrophyRecord(
            ImmutableSet.of(145, 146, 495, 540),
            "bands%n", "Bands %r", "Collect %n Music Band sets",
            3, 5, 8, 12, 17),
        new TrophyRecord(
            ImmutableSet.of(234, 437, 487),
            "albums%n", "Albums %r", "Collect %n Music Album sets",
            3, 5, 8, 12, 17),
        new TrophyRecord(
            ImmutableSet.of(166, 177, 219, 231, 274, 276, 283, 290, 449, 461, 465, 479, 480, 481,
                482, 483, 486, 488, 489, 490, 493),
            "television%n", "Television %r", "Collect %n Television sets",
            3, 5, 8, 12, 17),
        new TrophyRecord(
            ImmutableSet.of(17, 98, 181, 235, 249, 257, 285, 289, 306, 355, 357, 362),
            "mammals%n", "Mammals %r", "Collect %n sets of mammals",
            3, 5, 8, 12, 17),
        new TrophyRecord(
            ImmutableSet.of(57, 97, 244, 312, 433, 441),
            "birds%n", "Birds %r", "Collect %n sets of birds",
            3, 5, 8, 12, 17),
        new TrophyRecord(
            ImmutableSet.of(56, 71, 126, 127, 128),
            "insects%n", "Insects %r", "Collect %n sets of insects",
            3, 5, 8, 12, 17),
        new TrophyRecord(
            ImmutableSet.of(317, 318, 319, 323, 324, 333, 334, 335, 336, 337, 338, 339, 340),
            "chemistry%n", "Chemistry %r", "Collect %n sets of chemicals",
            3, 5, 8, 12, 17)
    );

    @Inject protected EverythingApp _app;
    @Inject protected GameRepository _gameRepo;
    @Inject protected KontagentLogic _kontLogic;
    @Inject protected PlayerLogic _playerLogic;
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected ThingLogic _thingLogic;
    @Inject protected ThingRepository _thingRepo;

    /** One hour in milliseconds. */
    protected static final long HOUR = 60*60*1000L;
}
