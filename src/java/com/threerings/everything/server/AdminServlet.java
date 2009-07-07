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
import com.threerings.everything.data.Series;
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
    public void deleteCategory (int categoryId) throws ServiceException
    {
        OOOUser admin = requireAdmin();
        if (_thingRepo.loadCategories(categoryId).iterator().hasNext()) {
            throw new ServiceException(AdminService.E_CAT_HAS_SUBCATS);
        }
        if (_thingRepo.loadSeries(categoryId).iterator().hasNext()) {
            throw new ServiceException(AdminService.E_CAT_HAS_SERIES);
        }
        log.info("Deleting category", "who", admin.username, "catId", categoryId);
        _thingRepo.deleteCategory(categoryId);
    }

    // from interface AdminService
    public List<Series> loadSeries (int categoryId) throws ServiceException
    {
        requireAdmin();
        return Lists.newArrayList(_thingRepo.loadSeries(categoryId));
    }

    // from interface AdminService
    public int createSeries (Series series) throws ServiceException
    {
        OOOUser admin = requireAdmin();
        return _thingRepo.createSeries(series, admin.userId);
    }

    // from interface AdminService
    public void deleteSeries (int seriesId) throws ServiceException
    {
        OOOUser admin = requireAdmin();
        if (_thingRepo.loadThings(seriesId).iterator().hasNext()) {
            throw new ServiceException(AdminService.E_SERIES_HAS_THINGS);
        }
        log.info("Deleting series", "who", admin.username, "seriesId", seriesId);
        _thingRepo.deleteSeries(seriesId);
    }

    // from interface AdminService
    public List<Thing> loadThings (int parentId) throws ServiceException
    {
        requireAdmin();
        return Lists.newArrayList(_thingRepo.loadThings(parentId));
    }

    // from interface AdminService
    public int createThing (Thing thing) throws ServiceException
    {
        OOOUser admin = requireAdmin();
        return _thingRepo.createThing(thing, admin.userId);
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
