//
// $Id$

package com.threerings.everything.server;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.CalendarUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.threerings.everything.client.GameCodes;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.Grid;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.Thing;
import com.threerings.everything.data.ThingCard;

import com.threerings.everything.server.persist.CardRecord;
import com.threerings.everything.server.persist.CategoryRecord;
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
        GameStatus status = PlayerRecord.TO_STATUS.apply(player);
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
        grid.thingIds = selectGridThings(player, Grid.GRID_SIZE);

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
        IntMap<ThingCard> cards = IntMaps.newHashIntMap();
        for (ThingCard card : _thingRepo.loadThingCards(new ArrayIntSet(record.thingIds))) {
            cards.put(card.thingId, card);
        }

        // load up the flipped status for the player's current grid
        boolean[] flipped = _gameRepo.loadFlipped(record.userId);

        // place the flipped cards into the runtime grid and remove them from our map
        for (int ii = 0; ii < record.thingIds.length; ii++) {
            if (flipped[ii]) {
                grid.flipped[ii] = cards.remove(record.thingIds[ii]);
            }
        }

        // now summarize the rarities of the remaining unflipped cards
        for (ThingCard card : cards.values()) {
            grid.unflipped[card.rarity.toByte()]++;
        }

        return grid;
    }

    /**
     * Resolves runtime card data for the supplied card.
     */
    public Card resolveCard (CardRecord record)
    {
        Card card = new Card();
        card.owner = _playerRepo.loadPlayerName(record.ownerId);
        card.thing = _thingRepo.loadThing(record.thingId);
        card.categories = resolveCategories(card.thing);
        if (record.giverId != 0) {
            card.giver = _playerRepo.loadPlayerName(record.giverId);
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
        return total / count;
    }

    /**
     * Computes the fractional number of free flips earned by a player who just started a session
     * after the specified number of milliseconds since their last session and who currently has
     * the specified number of free flips.
     */
    public float computeFreeFlipsEarned (float haveFlips, long elapsed)
    {
        // if they have fewer than the daily free flips grant, compute how many millis would be
        // needed to top them up at that rate
        float earnedFlips = 0;
        if (haveFlips < GameCodes.DAILY_FREE_FLIPS) {
            long millisPerFlip = ONE_DAY / GameCodes.DAILY_FREE_FLIPS;
            float allowedDaily = GameCodes.DAILY_FREE_FLIPS - haveFlips;
            long allowedMillis = Math.round(allowedDaily * millisPerFlip);
            if (elapsed > allowedMillis) {
                // they've been gone more than a day: accumulate up to the daily limit at the daily
                // rate and then fall through and accumulate further flips at the vacation rate
                earnedFlips += allowedDaily;
                elapsed -= allowedMillis;
            } else {
                // they've been gone less than a day, so accumulate at the daily rate
                return elapsed / (float)millisPerFlip;
            }
        }
        // now we're accumulating at the vacation rate
        return earnedFlips + GameCodes.VACATION_FREE_FLIPS * elapsed / (float)ONE_DAY;
    }

    /**
     * Selects the things that will be contained in a fresh grid for the specified player. The
     * things will be selected using our most recently loaded snapshot of the thing database based
     * on the aggregate rarities of all of the things in that snapshot.
     */
    protected int[] selectGridThings (PlayerRecord player, int count)
    {
        // TODO: see if this player has any active powerups, apply them during selection
        return getThingIndex().selectThings(count);
    }

    /**
     * Resolves the categories for the specified thing.
     */
    protected Category[] resolveCategories (Thing thing)
    {
        List<Category> cats = Lists.newArrayList();
        int categoryId = thing.categoryId;
        while (categoryId != 0) {
            Category cat = _thingRepo.loadCategory(categoryId);
            if (cat == null) {
                log.warning("Missing category for thing!",  "thing", thing.thingId,
                            "categoryId", categoryId);
                categoryId = 0;
            } else {
                cats.add(cat);
                categoryId = cat.parentId;
            }
        }
        return cats.toArray(new Category[cats.size()]);
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
        for (CategoryRecord catrec : _thingRepo.loadActiveCategories()) {
            if (catrec.parentId != 0) {
                catmap.put(catrec.categoryId, catrec.parentId);
            }
        }
        return new ThingIndex(catmap, _thingRepo.loadActiveThings());
    }

    protected ThingIndex _index;
    protected long _nextIndexUpdate;

    @Inject protected GameRepository _gameRepo;
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected ThingRepository _thingRepo;

    /** The minimum allowed lifespan for a grid. */
    protected static final long MIN_GRID_DURATION = 2 * 60 * 60 * 1000L;

    /** Recompute our thing index every five minutes. */
    protected static final long THING_INDEX_UPDATE_INTERVAL = 5 * 60 * 1000L;

    /** One day in milliseconds. */
    protected static final long ONE_DAY = 24 * 60 * 60 * 1000L;
}
