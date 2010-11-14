//
// $Id$

package client.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.Callbacks;

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
            _editorsvc.loadCategories(parentId, Callbacks.before(callback, storeOp));
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
            cat, Callbacks.map(Callbacks.before(callback, addCatOp), toCat));
    }

    /**
     * Reparents the supplied category with the specified new parent.
     */
    public void moveCategory (final Category cat, final int newParentId,
                              AsyncCallback<Void> callback)
    {
        final int oldParentId = cat.parentId;
        Function<Void, Void> movedOp = new Function<Void, Void>() {
            public Void apply (Void result) {
                List<Category> cats = _catmap.get(oldParentId);
                if (cats != null) {
                    cats.remove(cat);
                }
                cats = _catmap.get(newParentId);
                if (cats != null) {
                    cats.add(cat);
                    Collections.sort(cats);
                }
                return null;
            }
        };
        cat.parentId = newParentId;
        _editorsvc.updateCategory(cat, Callbacks.before(callback, movedOp));
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
        _editorsvc.deleteCategory(categoryId, Callbacks.before(callback, deletedOp));
    }

    /**
     * Notes that a thing was added to the specified series.
     */
    public void thingAdded (Category series)
    {
        // locate the series in its parent's list
        List<Category> cats = _catmap.get(series.parentId);
        if (cats != null) {
            for (Category cat : cats) {
                if (cat.categoryId == series.categoryId) {
                    cat.things++;
                    break;
                }
            }
        }
    }

    protected Context _ctx;
    protected Map<Integer, List<Category>> _catmap = new HashMap<Integer, List<Category>>();

    protected static final EditorServiceAsync _editorsvc = GWT.create(EditorService.class);
}
