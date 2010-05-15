//
// $Id$

package com.threerings.everything.client;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.samsara.app.client.ServiceException;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.CategoryComment;
import com.threerings.everything.data.PendingSeries;
import com.threerings.everything.data.Thing;

/**
 * Provides editor services to the Everything client.
 */
@RemoteServiceRelativePath(EditorService.ENTRY_POINT)
public interface EditorService extends RemoteService
{
    /** The path at which this servlet is mapped. */
    public static final String ENTRY_POINT = "editor";

    /** Thrown by {@link #deleteCategory} if the category in question has subcategories. */
    public static final String E_CAT_HAS_SUBCATS = "e.cat_has_subcats";

    /** Thrown by {@link #deleteCategory} if the category in question has things. */
    public static final String E_CAT_HAS_THINGS = "e.cat_has_things";

    /** Thrown by {@link #deleteThing} if the thing in question is used in cards. */
    public static final String E_THING_IN_CARDS = "e.thing_in_cards";

    /** Thrown by {@link #slurpImage} if the supplied image URL is invalid. */
    public static final String E_INVALID_URL = "e.invalid_url";

    /** Provides results for {@link #loadSeries}. */
    public static class SeriesResult implements IsSerializable
    {
        /** The categories of this series. */
        public Category[] categories;

        /** The comments made on this series. */
        public List<CategoryComment> comments;

        /** The things in this series. */
        public List<Thing> things;
    }

    /**
     * Loads all of the categories with the specified parent. Specifying a parentId of 0 will load
     * all top-level categories.
     */
    List<Category> loadCategories (int parentId) throws ServiceException;

    /**
     * Loads the list of series created by the caller (pending and shipped).
     */
    List<Category> loadMySeries () throws ServiceException;

    /**
     * Loads the list of series pending approval.
     */
    List<PendingSeries> loadPendingSeries () throws ServiceException;

    /**
     * Creates a new category with the specified configuration.
     *
     * @return the new category's id.
     */
    int createCategory (Category category) throws ServiceException;

    /**
     * Updates the specified category.
     */
    void updateCategory (Category category) throws ServiceException;

    /**
     * Deletes the specified category, which must have no child categories, nor things.
     */
    void deleteCategory (int categoryId) throws ServiceException;

    /**
     * Posts a comment about this category which shows up in its creator's feed.
     */
    CategoryComment postComment (int categoryId, String message) throws ServiceException;

    /**
     * Loads all of the things in the specified series.
     */
    SeriesResult loadSeries (int categoryId) throws ServiceException;

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

    /**
     * Requests that the server slurp down the specified image, upload it to our media repository
     * and return the path to the slurped media.
     */
    String slurpImage (String imgurl) throws ServiceException;

    /**
     * Updates callers vote for the specified pending series.
     */
    void updatePendingVote (int categoryId, boolean inFavor) throws ServiceException;
}
