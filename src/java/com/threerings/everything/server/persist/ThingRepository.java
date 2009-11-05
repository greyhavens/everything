//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.CountRecord;
import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Funcs;
import com.samskivert.depot.Key;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.FieldOverride;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntSet;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.CategoryComment;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.SeriesCard;
import com.threerings.everything.data.Thing;
import com.threerings.everything.data.ThingCard;
import com.threerings.everything.data.ThingStats;

/**
 * Manages category and thing data for the Everything app.
 */
@Singleton
public class ThingRepository extends DepotRepository
{
    @Inject public ThingRepository (PersistenceContext ctx)
    {
        super(ctx);

        // temp: migrate "active" to "state = ACTIVE"
        registerMigration(new DataMigration("2009_07_29_category_active") {
            public void invoke () throws DatabaseException {
                updatePartial(CategoryRecord.class, new Where(CategoryRecord.ACTIVE.eq(true)),
                              null, CategoryRecord.STATE, Category.State.ACTIVE);
            }
        });
    }

    /**
     * Loads and returns stats on the thing database.
     */
    public ThingStats loadStats ()
    {
        ThingStats stats = new ThingStats();
        stats.totalThings = load(CountRecord.class, new FromOverride(ThingRecord.class)).count;
        stats.totalCategories =
            load(CountRecord.class, new FromOverride(CategoryRecord.class)).count;
        stats.totalPlayers = load(CountRecord.class, new FromOverride(PlayerRecord.class)).count;
        stats.totalCards = load(CountRecord.class, new FromOverride(CardRecord.class)).count;
        return stats;
    }

    /**
     * Loads and returns the specified category or null if none exists with that id.
     */
    public Category loadCategory (int categoryId)
    {
        return CategoryRecord.TO_CATEGORY.apply(load(CategoryRecord.getKey(categoryId)));
    }

    /**
     * Loads and returns all category records with the specified parent id.
     */
    public Collection<Category> loadCategories (int parentId)
    {
        return findAll(CategoryRecord.class, new Where(CategoryRecord.PARENT_ID.eq(parentId))).
            map(CategoryRecord.TO_CATEGORY);
    }

    /**
     * Loads all categories with ids in the supplied set.
     */
    public Collection<Category> loadCategories (Set<Integer> ids)
    {
        return findAll(CategoryRecord.class, new Where(CategoryRecord.CATEGORY_ID.in(ids))).
            map(CategoryRecord.TO_CATEGORY);
    }

    /**
     * Loads all categories that are not yet marked active.
     */
    public Collection<Category> loadPendingCategories ()
    {
        return findAll(CategoryRecord.class,
                       CategoryRecord.CATEGORY_ID.join(ThingRecord.CATEGORY_ID),
                       new Where(CategoryRecord.STATE.notEq(Category.State.ACTIVE)),
                       new GroupBy(CategoryRecord.CATEGORY_ID)).
            map(CategoryRecord.TO_CATEGORY);
    }

    /**
     * Loads and returns all categories.
     */
    public Collection<Category> loadAllCategories ()
    {
        return findAll(CategoryRecord.class).map(CategoryRecord.TO_CATEGORY);
    }

    /**
     * Returns a mapping from editor to total number of things in active series created by that
     * editor.
     */
    public IntIntMap loadEditorInfo ()
    {
        IntIntMap info = new IntIntMap();
        Where where = new Where(CategoryRecord.STATE.eq(Category.State.ACTIVE));
        for (CategoryRecord catrec : findAll(CategoryRecord.class, where)) {
            info.increment(catrec.creatorId, catrec.things);
        }
        return info;
    }

    /**
     * Creates a new category.
     *
     * @return the category's newly assigned id.
     */
    public int createCategory (Category category)
    {
        CategoryRecord record = CategoryRecord.FROM_CATEGORY.apply(category);
        insert(record); // assigns record.categoryId
        return record.categoryId;
    }

    /**
     * Updates an existing category. Returns true if a category was found and updated, false
     * othwerwise.
     */
    public boolean updateCategory (Category category)
    {
        return update(CategoryRecord.FROM_CATEGORY.apply(category)) == 1;
    }

    /**
     * Deletes the specified category. The caller is responsible for making sure this is a good
     * idea.
     */
    public void deleteCategory (Category category)
    {
        delete(CategoryRecord.getKey(category.categoryId));
        deleteAll(CategoryCommentRecord.class,
                  new Where(CategoryCommentRecord.CATEGORY_ID.eq(category.categoryId)), null);
    }

    /**
     * Loads all comments made on the specified category, ordered from most to least recent.
     */
    public Collection<CategoryComment> loadComments (int categoryId)
    {
        return findAll(CategoryCommentRecord.class,
                       new Where(CategoryCommentRecord.CATEGORY_ID.eq(categoryId)),
                       OrderBy.descending(CategoryCommentRecord.WHEN)).
            map(CategoryCommentRecord.TO_COMMENT);
    }

    /**
     * Loads all comments made since the specified cutoff in any category created by the specified
     * user, ordered from most to least recent.
     */
    public Collection<CategoryComment> loadCommentsSince (int creatorId, long sinceStamp)
    {
        Timestamp since = new Timestamp(sinceStamp);
        return findAll(CategoryCommentRecord.class,
                       CategoryCommentRecord.CATEGORY_ID.join(CategoryRecord.CATEGORY_ID),
                       new Where(Ops.and(CategoryRecord.CREATOR_ID.eq(creatorId),
                                         CategoryCommentRecord.WHEN.greaterEq(since))),
                       OrderBy.descending(CategoryCommentRecord.WHEN)).
            map(CategoryCommentRecord.TO_COMMENT);
    }

    /**
     * Records a comment to a category. Returns the newly created comment record.
     */
    public CategoryComment recordComment (int categoryId, int commentorId, String message)
    {
        CategoryCommentRecord record = new CategoryCommentRecord();
        record.categoryId = categoryId;
        record.when = new Timestamp(System.currentTimeMillis());
        record.commentorId = commentorId;
        record.message = message;
        insert(record);
        return CategoryCommentRecord.TO_COMMENT.apply(record);
    }

    /**
     * Loads and returns the specified thing, or null if no thing exists with that id.
     */
    public Thing loadThing (int thingId)
    {
        return ThingRecord.TO_THING.apply(load(ThingRecord.getKey(thingId)));
    }

    /**
     * Loads and returns the specified things.
     */
    public Collection<Thing> loadThings (Set<Integer> thingIds)
    {
        return findAll(ThingRecord.class, new Where(ThingRecord.THING_ID.in(thingIds))).
            map(ThingRecord.TO_THING);
    }

    /**
     * Loads and returns all things in the specified category.
     */
    public Collection<Thing> loadThings (int categoryId)
    {
        return findAll(ThingRecord.class, new Where(ThingRecord.CATEGORY_ID.eq(categoryId))).
            map(ThingRecord.TO_THING);
    }

    /**
     * Loads and returns cards for all things in the specified category.
     */
    public Collection<ThingCard> loadThingCards (int categoryId)
    {
        return findAll(ThingRecord.class, new Where(ThingRecord.CATEGORY_ID.eq(categoryId))).
            map(ThingRecord.TO_CARD);
    }

    /**
     * Returns the number of things in the specified category. Only valid for leaf categories.
     */
    public int getThingCount (int categoryId)
    {
        return load(CountRecord.class,
                    new FromOverride(ThingRecord.class),
                    new Where(ThingRecord.CATEGORY_ID.eq(categoryId))).count;
    }

    /**
     * Creates a new thing.
     *
     * @return the thing's newly assigned unique id.
     */
    public int createThing (Thing thing)
    {
        ThingRecord record = ThingRecord.FROM_THING.apply(thing);
        insert(record); // assigns record.thingId
        updatePartial(CategoryRecord.getKey(thing.categoryId),
                      CategoryRecord.THINGS, getThingCount(thing.categoryId));
        return record.thingId;
    }

    /**
     * Updates an existing thing. Returns true if a thing was found and updated, false othwerwise.
     */
    public boolean updateThing (Thing thing)
    {
        return update(ThingRecord.FROM_THING.apply(thing)) == 1;
    }

    /**
     * Deletes the specified thing. The caller is responsible for making sure this is a good idea.
     */
    public void deleteThing (Thing thing)
    {
        delete(ThingRecord.getKey(thing.thingId));
        updatePartial(CategoryRecord.getKey(thing.categoryId),
                      CategoryRecord.THINGS, getThingCount(thing.categoryId));
    }

    /**
     * Loads summary information on every active thing in the repository.
     */
    public Collection<ThingInfoRecord> loadActiveThings ()
    {
        return findAll(ThingInfoRecord.class,
                       CacheStrategy.NONE,
                       ThingRecord.CATEGORY_ID.join(CategoryRecord.CATEGORY_ID),
                       new Where(CategoryRecord.STATE.eq(Category.State.ACTIVE)));
    }

    /**
     * Load all the thing ids owned by this player.
     */
    public IntSet loadPlayerThings (int ownerId)
    {
        return loadPlayerThings(ownerId, null, null);
    }

    /**
     * Loads the thing ids of this player's things that are the specified rarity or higher.
     */
    public IntSet loadPlayerThings (int ownerId, Rarity minRarity)
    {
        return loadPlayerThings(ownerId, minRarity, null);
    }

    /**
     * Loads the thing ids of this player's things, with optional min and max rarities.
     */
    public IntSet loadPlayerThings (int ownerId, Rarity minRarity, Rarity maxRarity)
    {
        List<SQLExpression> whereConds = Lists.newArrayList();
        whereConds.add(CardRecord.OWNER_ID.eq(ownerId));
        if (minRarity != null) {
            whereConds.add(ThingRecord.RARITY.greaterEq(minRarity));
        }
        if (maxRarity != null) {
            whereConds.add(ThingRecord.RARITY.lessEq(maxRarity));
        }
        return new ArrayIntSet(
            findAllKeys(ThingRecord.class, false,
                        ThingRecord.THING_ID.join(CardRecord.THING_ID),
                        new Where(Ops.and(whereConds))).
            map(Key.<ThingRecord>toInt()));
    }

    /**
     * Loads the category id and count of unique things owned by the player in each category.
     */
    public IntIntMap loadPlayerSeriesInfo (int ownerId)
    {
        IntIntMap owned = new IntIntMap();
        for (OwnedRecord orec : findAll(OwnedRecord.class,
                                        CategoryRecord.CATEGORY_ID.join(ThingRecord.CATEGORY_ID),
                                        ThingRecord.THING_ID.join(CardRecord.THING_ID),
                                        new FieldOverride(OwnedRecord.OWNED,
                                                          Funcs.countDistinct(CardRecord.THING_ID)),
                                        new GroupBy(CategoryRecord.CATEGORY_ID),
                                        new Where(CardRecord.OWNER_ID.eq(ownerId)))) {
            owned.put(orec.categoryId, orec.owned);
        }
        return owned;
    }

    /**
     * Loads data on all series owned by the specified player.
     */
    public List<SeriesCard> loadPlayerSeries (int ownerId)
    {
        IntIntMap owned = loadPlayerSeriesInfo(ownerId);
        List<SeriesCard> cards = Lists.newArrayList();
        Where where = new Where(CategoryRecord.CATEGORY_ID.in(owned.keySet()));
        for (CategoryRecord crec : findAll(CategoryRecord.class, where)) {
            SeriesCard card = CategoryRecord.TO_SERIES_CARD.apply(crec);
            card.owned = owned.getOrElse(crec.categoryId, 0);
            cards.add(card);
        }
        return cards;
    }

    protected void updateCategoryThingCount (int categoryId)
    {
        updatePartial(CategoryRecord.getKey(categoryId),
                      CategoryRecord.THINGS, getThingCount(categoryId));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CategoryCommentRecord.class);
        classes.add(CategoryRecord.class);
        classes.add(ThingRecord.class);
    }
}
