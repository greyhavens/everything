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

import com.threerings.everything.client.AdminService;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.Created;
import com.threerings.everything.data.Thing;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.ThingRepository;

import static com.threerings.everything.Log.log;

/**
 * Implements {@link AdminService}.
 */
public class AdminServlet extends EveryServiceServlet
    implements AdminService
{
    // from interface AdminService
    public List<Category> loadCategories (int parentId) throws ServiceException
    {
        requireAdmin();
        return sort(Lists.newArrayList(_thingRepo.loadCategories(parentId)));
    }

    // from interface AdminService
    public int createCategory (Category category) throws ServiceException
    {
        PlayerRecord editor = requireEditor();
        category.creatorId = editor.userId;
        category.categoryId = _thingRepo.createCategory(category);
        _adminLogic.noteAction(editor, "created", category);
        return category.categoryId;
    }

    // from interface AdminService
    public void updateCategory (Category category) throws ServiceException
    {
        // make sure they have editing privileges on this object (and that it exists)
        Category ocategory = _thingRepo.loadCategory(category.categoryId);
        PlayerRecord editor = checkEditor(ocategory);

        // only admins can activate categories
        if (!ocategory.active && category.active) {
            requireAdmin();
        }

        // actually update the category
        _thingRepo.updateCategory(category);

        String action;
        if (ocategory.active != category.active) {
            action = (category.active) ? "activated" : "deactivated";
            // if a category changed active state, queue a reload of the thing index
            log.info("Category active status changed. TODO: reload thing index.");
        } else {
            action = "updated";
        }

        // note that this action was taken
        _adminLogic.noteAction(editor, action, category);
    }

    // from interface AdminService
    public void deleteCategory (int categoryId) throws ServiceException
    {
        // make sure they have editing privileges on this object (and that it exists)
        PlayerRecord editor = checkEditor(_thingRepo.loadCategory(categoryId));

        // make sure the category can be deleted
        if (_thingRepo.loadCategories(categoryId).iterator().hasNext()) {
            throw new ServiceException(AdminService.E_CAT_HAS_SUBCATS);
        }
        if (_thingRepo.loadThings(categoryId).iterator().hasNext()) {
            throw new ServiceException(AdminService.E_CAT_HAS_THINGS);
        }

        // delete the category
        _thingRepo.deleteCategory(categoryId);

        // note that this action was taken
        _adminLogic.noteDeleted(editor, "category " + categoryId);
    }

    // from interface AdminService
    public List<Thing> loadThings (int parentId) throws ServiceException
    {
        requireAdmin();
        return sort(Lists.newArrayList(_thingRepo.loadThings(parentId)));
    }

    // from interface AdminService
    public int createThing (Thing thing) throws ServiceException
    {
        PlayerRecord editor = requireEditor();
        thing.creatorId = editor.userId;
        thing.thingId = _thingRepo.createThing(thing);
        _adminLogic.noteAction(editor, "created", thing);
        return thing.thingId;
    }

    // from interface AdminService
    public void updateThing (Thing thing) throws ServiceException
    {
        // make sure they have editing privileges on this object (and that it exists)
        PlayerRecord editor = checkEditor(_thingRepo.loadThing(thing.thingId));

        // actually update the thing
        _thingRepo.updateThing(thing);

        // note that this action was taken
        _adminLogic.noteAction(editor, "updated", thing);
    }

    // from interface AdminService
    public void deleteThing (int thingId) throws ServiceException
    {
        // make sure they have editing privileges on this object (and that it exists)
        PlayerRecord editor = checkEditor(_thingRepo.loadThing(thingId));

        // TODO: check whether cards are using this thing

        // delete the thing
        _thingRepo.deleteThing(thingId);

        // note that this action was taken
        _adminLogic.noteDeleted(editor, "thing " + thingId);
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
    @Inject protected ThingRepository _thingRepo;
}
