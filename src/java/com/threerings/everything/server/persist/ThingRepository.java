//
// $Id$

package com.threerings.everything.server.persist;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.Rarity;
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
     * Creates a new category.
     *
     * @return the category's newly assigned id.
     */
    public int createCategory (Category category, int creatorId)
    {
        CategoryRecord record = CategoryRecord.FROM_CATEGORY.apply(category);
        record.creatorId = creatorId;
        insert(record); // assigns record.categoryId
        return record.categoryId;
    }

    /**
     * Deletes the specified category. The caller is responsible for making sure this is a good
     * idea.
     */
    public void deleteCategory (int categoryId)
    {
        delete(CategoryRecord.getKey(categoryId));
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
     * Creates a new thing.
     */
    public int createThing (Thing thing, int creatorId)
    {
        ThingRecord record = ThingRecord.FROM_THING.apply(thing);
        record.creatorId = creatorId;
        insert(record); // assigns record.thingId
        return record.thingId;
    }

    /**
     * Updates an existing thing. Returns true if a thing was found and updated, false othwerwise.
     */
    public boolean updateThing (Thing thing, int editorId)
    {
        ThingRecord orecord = load(ThingRecord.getKey(thing.thingId));
        if (orecord == null) {
            return false;
        }
        ThingRecord nrecord = ThingRecord.FROM_THING.apply(thing);
        nrecord.creatorId = orecord.creatorId;
        update(nrecord);
        return true;
    }

    /**
     * Deletes the specified thing. The caller is responsible for making sure this is a good idea.
     */
    public void deleteThing (int thingId)
    {
        delete(ThingRecord.getKey(thingId));
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

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CategoryRecord.class);
        classes.add(ThingRecord.class);
    }
}
