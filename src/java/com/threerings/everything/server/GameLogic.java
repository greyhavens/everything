//
// $Id$

package com.threerings.everything.server;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.CalendarUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;

import com.threerings.everything.client.GameCodes;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Grid;
import com.threerings.everything.data.GridStatus;
import com.threerings.everything.data.Rarity;
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
    public GridRecord generateGrid (PlayerRecord player, GridRecord previous)
    {
        GridRecord grid = new GridRecord();
        grid.userId = player.userId;
        grid.gridId = (previous == null) ? 1 : previous.gridId + 1;
        grid.thingIds = selectGridThings(player);
        grid.status = GridStatus.NORMAL;

        // grids generally expire at midnight in the player's timezone
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(player.timezone));
        CalendarUtil.zeroTime(cal);
        cal.add(Calendar.DATE, 1);
        // if the grid generated won't live for at least two hours, push its expiry out a day
        long duration = cal.getTimeInMillis() - System.currentTimeMillis();
        if (duration < MIN_GRID_DURATION) {
            log.info("Rolling grid forward an extra day", "who", player.who(),
                     "zone", player.timezone, "had", duration, "odate", cal.getTime());
            cal.add(Calendar.DATE, 1);
        }
        grid.expires = new Timestamp(cal.getTimeInMillis());

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
        for (Thing thing : _thingRepo.loadThings(new ArrayIntSet(record.thingIds))) {
            things.put(thing.thingId, thing);
        }

        // load up the flipped status for the player's current grid
        boolean[] flipped = _gameRepo.loadFlipped(record.userId);

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
            } else if (flipped[ii]) {
                grid.flipped[ii] = thing.toCard();
            } else {
                grid.unflipped[thing.rarity.toByte()]++;
                if (reveal != null) {
                    grid.flipped[ii] = ThingCard.newPartial(reveal.get(thing.thingId));
                }
            }
        }

        return grid;
    }

    /**
     * Resolves runtime card data for the supplied card.
     */
    public Card resolveCard (CardRecord record, Thing thing, SortedSet<Thing> things)
    {
        Card card = new Card();
        card.owner = _playerRepo.loadPlayerName(record.ownerId);
        card.thing = thing;
        card.categories = resolveCategories(card.thing.categoryId);
        card.created = new Date(record.created.getTime());
        if (record.giverId != 0) {
            card.giver = _playerRepo.loadPlayerName(record.giverId);
        }
        // we have to load all the things in this category for this bit
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
    public void giftCard (PlayerRecord owner, CardRecord card, PlayerRecord target, String message)
    {
        // transfer the card to the target player
        int targetId = target.userId;
        _gameRepo.giftCard(card, targetId);

        // record that this player gifted this card
        Thing thing = _thingRepo.loadThing(card.thingId);
        _playerRepo.recordFeedItem(
            owner.userId, FeedItem.Type.GIFTED, targetId, thing.name, message);

        // send a Facebook notification to the recipient (TODO: localization?)
        String feedmsg = String.format(
            "gave you the <a href=\"%s\">%s</a> card in <a href=\"%s\">Everything</a>.",
            _app.getFacebookAppURL("BROWSE", targetId, thing.categoryId),
            thing.name, _app.getFacebookAppURL());
        if (!StringUtil.isBlank(message)) {
            // TODO: escape HTML
            feedmsg += " They said '" + message + "'.";
        }
        _playerLogic.sendFacebookNotification(owner, target, feedmsg);

        // check whether the recipient just completed a set
        Set<Integer> things = Sets.newHashSet(
            Iterables.transform(_thingRepo.loadThings(thing.categoryId), Functions.THING_ID));
        Set<Integer> holdings = Sets.newHashSet(
            Iterables.transform(_gameRepo.loadCards(targetId, things), Functions.CARD_THING_ID));
        holdings.add(card.thingId);
        if (things.size() - holdings.size() == 0) {
            maybeReportCompleted(targetId, _thingRepo.loadCategory(thing.categoryId));
        }
    }

    /**
     * Records and reports that the specified player completed the specified series if they haven't
     * already completed the series.
     */
    public void maybeReportCompleted (int userId, Category series)
    {
        if (_gameRepo.noteCompletedSeries(userId, series.categoryId)) {
            log.info("Player completed series!", "who", userId, "series", series.name);
            _playerRepo.recordFeedItem(userId, FeedItem.Type.COMPLETED, 0, series.name, null);
        }
    }

    /**
     * Selects the things that will be contained in a fresh grid for the specified player. The
     * things will be selected using our most recently loaded snapshot of the thing database based
     * on the aggregate rarities of all of the things in that snapshot.
     */
    protected int[] selectGridThings (PlayerRecord player)
    {
        ThingIndex index = getThingIndex();
        IntSet thingIds = new ArrayIntSet();

        // TODO: see if this player has any active powerups, apply them during selection

        // load up this player's collection summary, identify incomplete series
        IntIntMap owned = _thingRepo.loadPlayerSeriesInfo(player.userId);
        ArrayIntSet ownedCats = new ArrayIntSet();
        for (IntIntMap.IntIntEntry entry : owned.entrySet()) {
            if (entry.getIntValue() < index.getCategorySize(entry.getIntKey())) {
                ownedCats.add(entry.getIntKey());
            }
        }

        // select up to half of our cards from series we are collecting
        int ownedCount = Math.min(ownedCats.size(), Grid.GRID_SIZE/2);
        index.selectThings(ownedCount, ownedCats, thingIds);

        // now select the remainder randomly from all possible things
        int randoCount = Grid.GRID_SIZE - thingIds.size();
        index.selectThings(randoCount, thingIds);

        // shuffle the resulting thing ids for maximum randosity
        int[] ids = thingIds.toIntArray();
        ArrayUtil.shuffle(ids);
        return ids;
    }

    /**
     * Returns a recently computed thing index. May block if the index needs updating.
     */
    protected ThingIndex getThingIndex ()
    {
        long now = System.currentTimeMillis();
        synchronized (this) {
            if (_index != null && now < _nextIndexUpdate) {
                return _index;
            }
            _nextIndexUpdate = now + THING_INDEX_UPDATE_INTERVAL;
            // before the first thing index is generated, all callers must block
            if (_index == null) {
                return (_index = createThingIndex());
            }
            // otherwise other callers can use the old index until we've generated the new one
        }

        ThingIndex index = createThingIndex();
        synchronized (this) {
            _index = index;
        }
        return index;
    }

    /**
     * Creates a new thing index by scanning the entire thing database. This is expensive.
     */
    protected ThingIndex createThingIndex ()
    {
        IntIntMap catmap = new IntIntMap();
        for (Category cat : _thingRepo.loadAllCategories()) {
            if (cat.parentId != 0) {
                catmap.put(cat.categoryId, cat.parentId);
            }
        }
        return new ThingIndex(catmap, _thingRepo.loadActiveThings());
    }

    /**
     * Resolves either nothing, the series, the sub-category or the category for the specified
     * collection of things. Returns a map from thing id to the resolved name.
     */
    protected IntMap<String> resolveReveal (Collection<Thing> things, int reductions)
    {
        // first create a mapping from thing to category
        IntMap<Category> cats = loadCategoryMap(
            Sets.newHashSet(Iterables.transform(things, Functions.CATEGORY_ID)));
        Map<Integer, Category> thingcat = Maps.newHashMap();
        for (Thing thing : things) {
            thingcat.put(thing.thingId, cats.get(thing.categoryId));
        }

        // now reduce the specified number of times
        while (reductions > 0) {
            cats = loadCategoryMap(
                Sets.newHashSet(Iterables.transform(thingcat.values(), Functions.PARENT_ID)));
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

    protected ThingIndex _index;
    protected long _nextIndexUpdate;

    @Inject protected EverythingApp _app;
    @Inject protected GameRepository _gameRepo;
    @Inject protected PlayerLogic _playerLogic;
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected ThingRepository _thingRepo;

    /** The minimum allowed lifespan for a grid. */
    protected static final long MIN_GRID_DURATION = 2 * 60 * 60 * 1000L;

    /** Recompute our thing index every five minutes. */
    protected static final long THING_INDEX_UPDATE_INTERVAL = 5 * 60 * 1000L;
}
