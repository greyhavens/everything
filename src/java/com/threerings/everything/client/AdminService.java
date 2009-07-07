//
// $Id$

package com.threerings.everything.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.samsara.app.client.ServiceException;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.Thing;

/**
 * Provides admin services to the Everything client.
 */
@RemoteServiceRelativePath(AdminService.ENTRY_POINT)
public interface AdminService extends RemoteService
{
    /** The path at which this servlet is mapped. */
    public static final String ENTRY_POINT = "admin";

    /** Thrown by {@link #deleteCategory} if the category in question has subcategories. */
    public static final String E_CAT_HAS_SUBCATS = "e.cat_has_subcats";

    /** Thrown by {@link #deleteCategory} if the category in question has things. */
    public static final String E_CAT_HAS_THINGS = "e.cat_has_things";

    /** Thrown by {@link #deleteThing} if the thing in question is used in cards. */
    public static final String E_THING_IN_CARDS = "e.thing_in_cards";

    /**
     * Loads all of the categories with the specified parent. Specifying a parentId of 0 will load
     * all top-level categories.
     */
    List<Category> loadCategories (int parentId) throws ServiceException;

    /**
     * Creates a new category with the specified configuration.
     *
     * @return the new category's id.
     */
    int createCategory (Category category) throws ServiceException;

    /**
     * Deletes the specified category, which must have no child categories, nor things.
     */
    void deleteCategory (int categoryId) throws ServiceException;

    /**
     * Loads all of the things in the specified category.
     */
    List<Thing> loadThings (int categoryId) throws ServiceException;

    /**
     * Creates a new thing with the specified configuration.
     *
     * @return the new thing's id.
     */
    int createThing (Thing thing) throws ServiceException;

    /**
     * Updates the supplied thing.
     */
    void updateThing (Thing thing) throws ServiceException;

    /**
     * Deletes the specified thing.
     */
    void deleteThing (int thingId) throws ServiceException;
}
