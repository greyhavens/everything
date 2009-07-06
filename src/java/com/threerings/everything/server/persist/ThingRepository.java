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
import com.threerings.everything.data.ThingSet;

/**
 * Manages thing and set data for the Everything app.
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
     */
    public Category createCategory (String name, int parentId, int creatorId)
    {
        CategoryRecord record = new CategoryRecord();
        record.name = name;
        record.parentId = parentId;
        record.creatorId = creatorId;
        insert(record); // assigns record.categoryId
        return record.toCategory();
    }

    /**
     * Loads and returns all set records with the specified category id.
     */
    public Iterable<ThingSet> loadSets (int categoryId)
    {
        return Iterables.transform(
            findAll(SetRecord.class, new Where(SetRecord.CATEGORY_ID.eq(categoryId))),
            SetRecord.TO_SET);
    }

    /**
     * Creates a new set.
     */
    public ThingSet createSet (String name, int categoryId, int creatorId)
    {
        SetRecord record = new SetRecord();
        record.categoryId = categoryId;
        record.name = name;
        record.creatorId = creatorId;
        insert(record); // assigns record.setId
        return record.toSet();
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CategoryRecord.class);
        classes.add(SetRecord.class);
        classes.add(ThingRecord.class);
    }
}
