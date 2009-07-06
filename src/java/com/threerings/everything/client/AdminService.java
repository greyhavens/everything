//
// $Id$

package com.threerings.everything.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.samsara.app.client.ServiceException;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.ThingSet;

/**
 * Provides admin services to the Everything client.
 */
@RemoteServiceRelativePath(AdminService.ENTRY_POINT)
public interface AdminService extends RemoteService
{
    /** The path at which this servlet is mapped. */
    public static final String ENTRY_POINT = "admin";

    /**
     * Loads all of the categories with the specified parent. Specifying a parentId of 0 will load
     * all top-level categories.
     */
    List<Category> loadCategories (int parentId) throws ServiceException;

    /**
     * Creates a new category with the specified name and parent id (which may be 0).
     */
    Category createCategory (String name, int parentId) throws ServiceException;

    /**
     * Loads all of the sets in the specified category.
     */
    List<ThingSet> loadSets (int categoryId) throws ServiceException;

    /**
     * Creates a new set with the specified name and category.
     */
    ThingSet createSet (String name, int categoryId) throws ServiceException;
}
