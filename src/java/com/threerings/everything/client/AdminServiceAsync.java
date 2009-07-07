//
// $Id$

package com.threerings.everything.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.Thing;
import com.threerings.everything.data.ThingSet;

/**
 * Provides the asynchronous version of {@link AdminService}.
 */
public interface AdminServiceAsync
{
    /**
     * The async version of {@link GameService#loadCategories}.
     */
    void loadCategories (int parentId, AsyncCallback<List<Category>> callback);

    /**
     * The async version of {@link GameService#createCategory}.
     */
    void createCategory (Category category, AsyncCallback<Integer> callback);

    /**
     * The async version of {@link GameService#deleteCategory}.
     */
    void deleteCategory (int categoryId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link GameService#loadSets}.
     */
    void loadSets (int categoryId, AsyncCallback<List<ThingSet>> callback);

    /**
     * The async version of {@link GameService#createSet}.
     */
    void createSet (ThingSet set, AsyncCallback<Integer> callback);

    /**
     * The async version of {@link GameService#deleteSet}.
     */
    void deleteSet (int setId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link GameService#loadThings}.
     */
    void loadThings (int parentId, AsyncCallback<List<Thing>> callback);

    /**
     * The async version of {@link GameService#createThing}.
     */
    void createThing (Thing thing, AsyncCallback<Integer> callback);

    /**
     * The async version of {@link GameService#deleteThing}.
     */
    void deleteThing (int thingId, AsyncCallback<Void> callback);
}
