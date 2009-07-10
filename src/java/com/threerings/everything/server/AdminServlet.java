//
// $Id$

package com.threerings.everything.server;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.server.AppServiceServlet;

import com.threerings.everything.client.AdminService;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.Thing;
import com.threerings.everything.server.persist.ThingRepository;

import static com.threerings.everything.Log.log;

/**
 * Implements {@link AdminService}.
 */
public class AdminServlet extends AppServiceServlet
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
        OOOUser admin = requireAdmin();
        return _thingRepo.createCategory(category, admin.userId);
    }

    // from interface AdminService
    public void updateCategory (Category category) throws ServiceException
    {
        OOOUser admin = requireAdmin();
        Category ocategory = _thingRepo.loadCategory(category.categoryId);
        _thingRepo.updateCategory(category, admin.userId);

        // if a category changed active state, queue a reload of the thing index
        if (ocategory.active != category.active) {
            log.info("Category active status changed. TODO: reload thing index.");
        }
    }

    // from interface AdminService
    public void deleteCategory (int categoryId) throws ServiceException
    {
        OOOUser admin = requireAdmin();
        if (_thingRepo.loadCategories(categoryId).iterator().hasNext()) {
            throw new ServiceException(AdminService.E_CAT_HAS_SUBCATS);
        }
        if (_thingRepo.loadThings(categoryId).iterator().hasNext()) {
            throw new ServiceException(AdminService.E_CAT_HAS_THINGS);
        }
        log.info("Deleting category", "who", admin.username, "catId", categoryId);
        _thingRepo.deleteCategory(categoryId);
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
        OOOUser admin = requireAdmin();
        return _thingRepo.createThing(thing, admin.userId);
    }

    // from interface AdminService
    public void updateThing (Thing thing) throws ServiceException
    {
        OOOUser admin = requireAdmin();
        _thingRepo.updateThing(thing, admin.userId);
    }

    // from interface AdminService
    public void deleteThing (int thingId) throws ServiceException
    {
        OOOUser admin = requireAdmin();
        // TODO: check whether cards are using this thing
        log.info("Deleting thing", "who", admin.username, "thingId", thingId);
        _thingRepo.deleteThing(thingId);
    }

    protected static <T extends Comparable<T>> List<T> sort (List<T> list) {
        Collections.sort(list);
        return list;
    }

    @Inject protected ThingRepository _thingRepo;
}
