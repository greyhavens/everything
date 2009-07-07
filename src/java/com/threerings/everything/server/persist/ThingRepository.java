//
// $Id$

package com.threerings.everything.server.persist;

import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.Thing;

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
     * Loads and returns all category records with the specified parent id.
     */
    public Iterable<Category> loadCategories (int parentId)
    {
        return Iterables.transform(
            findAll(CategoryRecord.class, new Where(CategoryRecord.PARENT_ID.eq(parentId))),
            CategoryRecord.TO_CATEGORY);
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
     * Loads and returns all things in the specified category.
     */
    public Iterable<Thing> loadThings (int categoryId)
    {
        return Iterables.transform(
            findAll(ThingRecord.class, new Where(ThingRecord.CATEGORY_ID.eq(categoryId))),
            ThingRecord.TO_THING);
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
     * Deletes the specified thing. The caller is responsible for making sure this is a good idea.
     */
    public void deleteThing (int thingId)
    {
        delete(ThingRecord.getKey(thingId));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CategoryRecord.class);
        classes.add(ThingRecord.class);
    }
}
