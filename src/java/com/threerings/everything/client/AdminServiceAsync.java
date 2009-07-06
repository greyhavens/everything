//
// $Id$

package com.threerings.everything.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.everything.data.Category;
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
    void createCategory (String name, int parentId, AsyncCallback<Category> callback);

    /**
     * The async version of {@link GameService#loadSets}.
     */
    void loadSets (int categoryId, AsyncCallback<List<ThingSet>> callback);

    /**
     * The async version of {@link GameService#createSet}.
     */
    void createSet (String name, int categoryId, AsyncCallback<ThingSet> callback);
}
