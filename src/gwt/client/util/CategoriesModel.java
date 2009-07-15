//
// $Id$

package client.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.ChainedCallback;
import com.threerings.gwt.util.Function;

import com.threerings.everything.client.EditorService;
import com.threerings.everything.client.EditorServiceAsync;
import com.threerings.everything.data.Category;

/**
 * Maintains data on the database of categories when editing.
 */
public class CategoriesModel
{
    public CategoriesModel (Context ctx)
    {
        _ctx = ctx;
    }

    /**
     * Returns the supplied categories from the model.
     */
    public void getCategories (final int parentId, AsyncCallback<List<Category>> callback)
    {
        List<Category> cats = _catmap.get(parentId);
        if (cats != null) {
            callback.onSuccess(cats);
        } else {
            Function<List<Category>, Void> storeOp = new Function<List<Category>, Void>() {
                public Void apply (List<Category> cats) {
                    _catmap.put(parentId, cats);
                    return null;
                }
            };
            _editorsvc.loadCategories(parentId, ChainedCallback.before(callback, storeOp));
        }
    }

    /**
     * Creates a new category, sending it to the server and adding it to the model.
     */
    public void createCategory (String name, int parentId, AsyncCallback<Category> callback)
    {
        final Category cat = new Category();
        cat.name = name;
        cat.parentId = parentId;
        cat.creator = _ctx.getMe();
        Function<Integer, Category> toCat = new Function<Integer, Category>() {
            public Category apply (Integer catId) {
                cat.categoryId = catId;
                return cat;
            }
        };
        Function<Category, Void> addCatOp = new Function<Category, Void>() {
            public Void apply (Category cat) {
                List<Category> cats = _catmap.get(cat.parentId);
                if (cats == null) {
                    _catmap.put(cat.parentId, cats = new ArrayList<Category>());
                }
                cats.add(cat);
                return null;
            }
        };
        _editorsvc.createCategory(
            cat, ChainedCallback.map(ChainedCallback.before(callback, addCatOp), toCat));
    }

    /**
     * Deletes a category, sending the request to the server and removing it from the model.
     */
    public void deleteCategory (final int categoryId, AsyncCallback<Void> callback)
    {
        Function<Void, Void> deletedOp = new Function<Void, Void>() {
            public Void apply (Void result) {
                _catmap.remove(categoryId);
                return null;
            }
        };
        _editorsvc.deleteCategory(categoryId, ChainedCallback.before(callback, deletedOp));
    }

    protected Context _ctx;
    protected Map<Integer, List<Category>> _catmap = new HashMap<Integer, List<Category>>();

    protected static final EditorServiceAsync _editorsvc = GWT.create(EditorService.class);
}