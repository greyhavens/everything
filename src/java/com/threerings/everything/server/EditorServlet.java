//
// $Id$

package com.threerings.everything.server;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.AppServiceServlet;

import com.threerings.everything.client.EditorService;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.Created;
import com.threerings.everything.data.Thing;
import com.threerings.everything.server.GameLogic;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.ThingRepository;

import static com.threerings.everything.Log.log;

/**
 * Implements {@link EditorService}.
 */
public class EditorServlet extends EveryServiceServlet
    implements EditorService
{
    // from interface EditorService
    public List<Category> loadCategories (int parentId) throws ServiceException
    {
        requireEditor();
        return sort(Lists.newArrayList(_thingRepo.loadCategories(parentId)));
    }

    // from interface EditorService
    public int createCategory (Category category) throws ServiceException
    {
        PlayerRecord editor = requireEditor();
        category.creator = editor.toName();
        category.categoryId = _thingRepo.createCategory(category);
        _adminLogic.noteAction(editor, "created", category);
        return category.categoryId;
    }

    // from interface EditorService
    public void updateCategory (Category category) throws ServiceException
    {
        // make sure they have editing privileges on this object (and that it exists)
        Category ocategory = _thingRepo.loadCategory(category.categoryId);
        PlayerRecord editor = checkEditor(ocategory);
        boolean activeChanged = (ocategory.active != category.active);

        // only editors can activate and deactivate categories
        if (activeChanged) {
            requireAdmin();
        }

        // the only updatable values are name, active and parentId
        ocategory.name = category.name;
        ocategory.active = category.active;
        ocategory.parentId = category.parentId;

        // actually update the category
        _thingRepo.updateCategory(ocategory);

        String action;
        if (activeChanged) {
            action = (category.active) ? "activated" : "deactivated";
            // if a category changed active state, queue a reload of the thing index
            log.info("Category active status changed. TODO: reload thing index.");
        } else {
            action = "updated";
        }

        // note that this action was taken
        _adminLogic.noteAction(editor, action, category);
    }

    // from interface EditorService
    public void deleteCategory (int categoryId) throws ServiceException
    {
        // make sure they have editing privileges on this object (and that it exists)
        Category category = _thingRepo.loadCategory(categoryId);
        PlayerRecord editor = checkEditor(category);

        // make sure the category can be deleted
        if (_thingRepo.loadCategories(categoryId).iterator().hasNext()) {
            throw new ServiceException(EditorService.E_CAT_HAS_SUBCATS);
        }
        if (_thingRepo.getThingCount(categoryId) > 0) {
            throw new ServiceException(EditorService.E_CAT_HAS_THINGS);
        }

        // delete the category
        _thingRepo.deleteCategory(category);

        // note that this action was taken
        _adminLogic.noteAction(editor, "deleted", category);
    }

    // from interface EditorService
    public SeriesResult loadSeries (int categoryId) throws ServiceException
    {
        requireEditor();
        SeriesResult result = new SeriesResult();
        result.categories = _gameLogic.resolveCategories(categoryId);
        result.things = sort(Lists.newArrayList(_thingRepo.loadThings(categoryId)));
        return result;
    }

    // from interface EditorService
    public int createThing (Thing thing) throws ServiceException
    {
        OOOUser user = requireUser();
        PlayerRecord editor = requireEditor(user);

        // make sure the series exists, they own it and that it's not active (these should be
        // checked on the client as well)
        Category series = _thingRepo.loadCategory(thing.categoryId);
        if (series == null) {
            throw new ServiceException(AppCodes.E_INTERNAL_ERROR);
        }
        if (series.active || (series.getCreatorId() != editor.userId && !user.isAdmin())) {
            throw new ServiceException(AppCodes.E_ACCESS_DENIED);
        }

        // do the deed
        thing.creator = editor.toName();
        thing.thingId = _thingRepo.createThing(thing);
        _adminLogic.noteAction(editor, "created", thing);
        return thing.thingId;
    }

    // from interface EditorService
    public void updateThing (Thing thing) throws ServiceException
    {
        // make sure they have editing privileges on this object (and that it exists)
        Thing othing = _thingRepo.loadThing(thing.thingId);
        PlayerRecord editor = checkEditor(othing);

        // TODO: if this thing is moving categories, validate that

        // actually update the thing
        thing.creator = othing.creator;
        _thingRepo.updateThing(thing);

        // note that this action was taken
        _adminLogic.noteAction(editor, "updated", thing);
    }

    // from interface EditorService
    public void deleteThing (int thingId) throws ServiceException
    {
        // make sure they have editing privileges on this object (and that it exists)
        Thing thing = _thingRepo.loadThing(thingId);
        PlayerRecord editor = checkEditor(thing);

        // TODO: check whether cards are using this thing

        // delete the thing
        _thingRepo.deleteThing(thing);

        // note that this action was taken
        _adminLogic.noteAction(editor, "deleted", thing);
    }

    protected PlayerRecord checkEditor (Created created)
        throws ServiceException
    {
        OOOUser user = requireUser();
        PlayerRecord editor = requireEditor(user);
        if (created == null) {
            log.warning("Requested to edit non-existent object", "who", editor.who());
            throw new ServiceException(AppCodes.E_INTERNAL_ERROR);
        }
        if (!user.isAdmin() && created.getCreatorId() != user.userId) {
            throw new ServiceException(AppCodes.E_ACCESS_DENIED);
        }
        return editor;
    }

    protected static <T extends Comparable<T>> List<T> sort (List<T> list) {
        Collections.sort(list);
        return list;
    }

    @Inject protected AdminLogic _adminLogic;
    @Inject protected GameLogic _gameLogic;
    @Inject protected ThingRepository _thingRepo;
}
