//
// $Id$

package com.threerings.everything.client;

import java.util.List;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.CategoryComment;
import com.threerings.everything.data.Thing;

/**
 * Provides the asynchronous version of {@link EditorService}.
 */
public interface EditorServiceAsync
{
    /**
     * The async version of {@link EditorService#loadCategories}.
     */
    void loadCategories (int parentId, AsyncCallback<List<Category>> callback);

    /**
     * The async version of {@link EditorService#createCategory}.
     */
    void createCategory (Category category, AsyncCallback<Integer> callback);

    /**
     * The async version of {@link EditorService#updateCategory}.
     */
    void updateCategory (Category category, AsyncCallback<Void> callback);

    /**
     * The async version of {@link EditorService#deleteCategory}.
     */
    void deleteCategory (int categoryId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link EditorService#postComment}.
     */
    void postComment (int categoryId, String message, AsyncCallback<CategoryComment> callback);

    /**
     * The async version of {@link EditorService#loadSeries}.
     */
    void loadSeries (int categoryId, AsyncCallback<EditorService.SeriesResult> callback);

    /**
     * The async version of {@link EditorService#createThing}.
     */
    void createThing (Thing thing, AsyncCallback<Integer> callback);

    /**
     * The async version of {@link EditorService#updateThing}.
     */
    void updateThing (Thing thing, AsyncCallback<Void> callback);

    /**
     * The async version of {@link EditorService#deleteThing}.
     */
    void deleteThing (int thingId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link EditorService#slurpImage}.
     */
    void slurpImage (String imgurl, AsyncCallback<String> callback);
}
