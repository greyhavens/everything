//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.CountRecord;
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
import com.samskivert.depot.util.Sequence;
import com.samskivert.util.CountMap;

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
    public Sequence<Category> loadCategories (int parentId)
    {
        return map(findAll(CategoryRecord.class,
            new Where(CategoryRecord.PARENT_ID.eq(parentId))),
            CategoryRecord.TO_CATEGORY);
    }

    /**
     * Loads all categories with ids in the supplied set.
     */
    public Sequence<Category> loadCategories (Set<Integer> ids)
    {
        return map(findAll(CategoryRecord.class, new Where(CategoryRecord.CATEGORY_ID.in(ids))),
            CategoryRecord.TO_CATEGORY);
    }

    /**
     * Loads all categories that are not yet marked active.
     */
    public Sequence<Category> loadPendingCategories ()
    {
        List<CategoryRecord> records = findAll(CategoryRecord.class,
            CategoryRecord.CATEGORY_ID.join(ThingRecord.CATEGORY_ID),
            new Where(CategoryRecord.STATE.notEq(Category.State.ACTIVE)),
            new GroupBy(CategoryRecord.CATEGORY_ID));
        return map(records, CategoryRecord.TO_CATEGORY);
    }

    /**
     * Loads all leaf categories created by the specified player.
     */
    public Iterable<Category> loadCategoriesBy (int creatorId)
    {
        // load all categories created by this player (will include non-leaves)
        Map<Integer, Category> cats = Maps.newHashMap();
        Set<Integer> parentIds = Sets.newHashSet();
        for (CategoryRecord crec : findAll(CategoryRecord.class,
                                           new Where(CategoryRecord.CREATOR_ID.eq(creatorId)))) {
            cats.put(crec.categoryId, CategoryRecord.TO_CATEGORY.apply(crec));
            parentIds.add(crec.parentId);
        }

        // now load up all parents of those categories, only those parents who also have parents
        // are sub-categories, meaning their children are leaves
        final Set<Integer> validParents = Sets.newHashSet();
        for (CategoryRecord crec : findAll(CategoryRecord.class,
                                           new Where(CategoryRecord.CATEGORY_ID.in(parentIds)))) {
            if (crec.parentId != 0) {
                validParents.add(crec.categoryId);
            }
        }

        // now return only those categories with valid parents
        return Iterables.filter(cats.values(), new Predicate<Category>() {
            public boolean apply (Category cat) {
                return validParents.contains(cat.parentId);
            }
        });
    }

    /**
     * Loads and returns all categories.
     */
    public Sequence<Category> loadAllCategories ()
    {
        return map(findAll(CategoryRecord.class), CategoryRecord.TO_CATEGORY);
    }

    /**
     * Returns a mapping from editor to total number of things in active series created by that
     * editor.
     */
    public CountMap<Integer> loadEditorInfo ()
    {
        CountMap<Integer> info = new CountMap<Integer>();
        Where where = new Where(CategoryRecord.STATE.eq(Category.State.ACTIVE));
        for (CategoryRecord catrec : findAll(CategoryRecord.class, where)) {
            info.add(catrec.creatorId, catrec.things);
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
    public Sequence<CategoryComment> loadComments (int categoryId)
    {
        List<CategoryCommentRecord> records = findAll(CategoryCommentRecord.class,
            new Where(CategoryCommentRecord.CATEGORY_ID.eq(categoryId)),
            OrderBy.descending(CategoryCommentRecord.WHEN));
        return map(records, CategoryCommentRecord.TO_COMMENT);
    }

    /**
     * Loads all comments made since the specified cutoff in any category created by the specified
     * user, ordered from most to least recent.
     */
    public Sequence<CategoryComment> loadCommentsSince (int creatorId, long sinceStamp)
    {
        Timestamp since = new Timestamp(sinceStamp);
        List<CategoryCommentRecord> records = findAll(CategoryCommentRecord.class,
            CategoryCommentRecord.CATEGORY_ID.join(CategoryRecord.CATEGORY_ID),
            new Where(Ops.and(CategoryRecord.CREATOR_ID.eq(creatorId),
                              CategoryCommentRecord.WHEN.greaterEq(since))),
            OrderBy.descending(CategoryCommentRecord.WHEN));
        return map(records, CategoryCommentRecord.TO_COMMENT);
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
    public Sequence<Thing> loadThings (Collection<Integer> thingIds)
    {
        return map(findAll(ThingRecord.class, new Where(ThingRecord.THING_ID.in(thingIds))),
            ThingRecord.TO_THING);
    }

    /**
     * Loads and returns all things in the specified category.
     */
    public Sequence<Thing> loadThings (int categoryId)
    {
        return map(findAll(ThingRecord.class, new Where(ThingRecord.CATEGORY_ID.eq(categoryId))),
            ThingRecord.TO_THING);
    }

    /**
     * Loads and returns cards for all things in the specified category.
     */
    public Sequence<ThingCard> loadThingCards (int categoryId)
    {
        return map(findAll(ThingRecord.class, new Where(ThingRecord.CATEGORY_ID.eq(categoryId))),
            ThingRecord.TO_CARD);
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
     * Loads information on currently-configured "attractor cards".
     */
    public Collection<AttractorRecord> loadAttractors ()
    {
        return findAll(AttractorRecord.class);
    }

    /**
     * Load all the thing ids owned by this player.
     */
    public Set<Integer> loadPlayerThings (int ownerId)
    {
        return loadPlayerThings(ownerId, null, null);
    }

    /**
     * Loads the thing ids of this player's things that are the specified rarity or higher.
     */
    public Set<Integer> loadPlayerThings (int ownerId, Rarity minRarity)
    {
        return loadPlayerThings(ownerId, minRarity, null);
    }

    /**
     * Loads the thing ids of this player's things, with optional min and max rarities.
     */
    public Set<Integer> loadPlayerThings (int ownerId, Rarity minRarity, Rarity maxRarity)
    {
        List<SQLExpression> whereConds = Lists.newArrayList();
        whereConds.add(CardRecord.OWNER_ID.eq(ownerId));
        if (minRarity != null) {
            whereConds.add(ThingRecord.RARITY.greaterEq(minRarity));
        }
        if (maxRarity != null) {
            whereConds.add(ThingRecord.RARITY.lessEq(maxRarity));
        }
        return Sets.newHashSet(map(findAllKeys(ThingRecord.class, false,
            ThingRecord.THING_ID.join(CardRecord.THING_ID),
            new Where(Ops.and(whereConds))), Key.<ThingRecord>toInt()));
    }

    /**
     * Loads the category id and count of unique things owned by the player in each category.
     */
    public CountMap<Integer> loadPlayerSeriesInfo (int ownerId)
    {
        CountMap<Integer> owned = new CountMap<Integer>();
        for (OwnedRecord orec : findAll(OwnedRecord.class,
                                        CategoryRecord.CATEGORY_ID.join(ThingRecord.CATEGORY_ID),
                                        ThingRecord.THING_ID.join(CardRecord.THING_ID),
                                        new FieldOverride(OwnedRecord.OWNED,
                                                          Funcs.countDistinct(CardRecord.THING_ID)),
                                        new GroupBy(CategoryRecord.CATEGORY_ID),
                                        new Where(CardRecord.OWNER_ID.eq(ownerId)))) {
            owned.add(orec.categoryId, orec.owned);
        }
        return owned;
    }

    /**
     * Loads data on all series owned by the specified player.
     */
    public List<SeriesCard> loadPlayerSeries (int ownerId)
    {
        CountMap<Integer> mySeriesCounts = loadPlayerSeriesInfo(ownerId);
        List<SeriesCard> cards = Lists.newArrayList();
        Where where = new Where(CategoryRecord.CATEGORY_ID.in(mySeriesCounts.keySet()));
        for (CategoryRecord crec : findAll(CategoryRecord.class, where)) {
            SeriesCard card = CategoryRecord.TO_SERIES_CARD.apply(crec);
            card.owned = mySeriesCounts.getCount(crec.categoryId);
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
        classes.add(AttractorRecord.class);
    }
}
