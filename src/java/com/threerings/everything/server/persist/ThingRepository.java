//
// $Id$

package com.threerings.everything.server.persist;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.CountRecord;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.util.IntIntMap;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.SeriesCard;
import com.threerings.everything.data.Thing;
import com.threerings.everything.data.ThingCard;

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
     * Loads and returns the specified category or null if none exists with that id.
     */
    public Category loadCategory (int categoryId)
    {
        return CategoryRecord.TO_CATEGORY.apply(load(CategoryRecord.getKey(categoryId)));
    }

    /**
     * Loads and returns all category records with the specified parent id.
     */
    public Iterable<Category> loadCategories (int parentId)
    {
        return findAll(CategoryRecord.class, new Where(CategoryRecord.PARENT_ID.eq(parentId))).
            map(CategoryRecord.TO_CATEGORY);
    }

    /**
     * Loads data on all series owned by the specified player.
     */
    public List<SeriesCard> loadPlayerSeries (int ownerId)
    {
        IntIntMap owned = new IntIntMap();
        for (OwnedRecord orec : findAll(OwnedRecord.class,
                                        CategoryRecord.CATEGORY_ID.join(ThingRecord.CATEGORY_ID),
                                        ThingRecord.THING_ID.join(CardRecord.THING_ID),
                                        new GroupBy(CategoryRecord.CATEGORY_ID),
                                        new Where(CardRecord.OWNER_ID.eq(ownerId)))) {
            owned.put(orec.categoryId, orec.owned);
        }

        // now load up the category info for those categories
        List<SeriesCard> cards = Lists.newArrayList();
        for (CategoryRecord crec :
                 findAll(CategoryRecord.class,
                         new Where(CategoryRecord.CATEGORY_ID.in(owned.keySet())))) {
            SeriesCard card = CategoryRecord.TO_SERIES_CARD.apply(crec);
            card.owned = owned.getOrElse(crec.categoryId, 0);
            cards.add(card);
        }
        return cards;
    }

    /**
     * Loads all categories with ids in the supplied set.
     */
    public Iterable<Category> loadCategories (Set<Integer> ids)
    {
        return findAll(CategoryRecord.class, new Where(CategoryRecord.CATEGORY_ID.in(ids))).
            map(CategoryRecord.TO_CATEGORY);
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
    }

    /**
     * Loads and returns the specified thing, or null if no thing exists with that id.
     */
    public Thing loadThing (int thingId)
    {
        return ThingRecord.TO_THING.apply(load(ThingRecord.getKey(thingId)));
    }

    /**
     * Loads and returns all things in the specified category.
     */
    public Iterable<Thing> loadThings (int categoryId)
    {
        return findAll(ThingRecord.class, new Where(ThingRecord.CATEGORY_ID.eq(categoryId))).
            map(ThingRecord.TO_THING);
    }

    /**
     * Loads and returns cards for the supplied set of things.
     */
    public Iterable<ThingCard> loadThingCards (Set<Integer> thingIds)
    {
        return findAll(ThingRecord.class, new Where(ThingRecord.THING_ID.in(thingIds))).
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
     * Loads all categories in the database that are active.
     */
    public Iterable<CategoryRecord> loadActiveCategories ()
    {
        return findAll(CategoryRecord.class, CacheStrategy.NONE,
                       new Where(CategoryRecord.ACTIVE.eq(true)));
    }

    /**
     * Loads summary information on every active thing in the repository.
     */
    public Iterable<ThingInfoRecord> loadActiveThings ()
    {
        return findAll(ThingInfoRecord.class,
                       CacheStrategy.NONE,
                       ThingRecord.CATEGORY_ID.join(CategoryRecord.CATEGORY_ID),
                       new Where(CategoryRecord.ACTIVE.eq(true)));
    }

    protected void updateCategoryThingCount (int categoryId)
    {
        updatePartial(CategoryRecord.getKey(categoryId),
                      CategoryRecord.THINGS, getThingCount(categoryId));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CategoryRecord.class);
        classes.add(ThingRecord.class);
    }
}
