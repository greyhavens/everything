//
// $Id$

package com.threerings.everything.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.threerings.user.OOOUser;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.server.AppServiceServlet;

import com.threerings.everything.client.AdminService;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.ThingSet;
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
        return Lists.newArrayList(_thingRepo.loadCategories(parentId));
    }

    // from interface AdminService
    public Category createCategory (String name, int parentId) throws ServiceException
    {
        OOOUser admin = requireAdmin();
        return _thingRepo.createCategory(name, parentId, admin.userId);
    }

    // from interface AdminService
    public List<ThingSet> loadSets (int categoryId) throws ServiceException
    {
        requireAdmin();
        return Lists.newArrayList(_thingRepo.loadSets(categoryId));
    }

    // from interface AdminService
    public ThingSet createSet (String name, int categoryId) throws ServiceException
    {
        OOOUser admin = requireAdmin();
        return _thingRepo.createSet(name, categoryId, admin.userId);
    }

    @Inject protected ThingRepository _thingRepo;
}
