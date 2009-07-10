//
// $Id$

package com.threerings.everything.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.everything.data.Action;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.Thing;
import com.threerings.everything.server.persist.AdminRepository;
import com.threerings.everything.server.persist.PlayerRecord;

/**
 * Provides admin services to server entities.
 */
@Singleton
public class AdminLogic
{
    public void noteAction (PlayerRecord editor, String action, Category category)
    {
        _adminRepo.recordAction(editor.userId, Action.Target.CATEGORY, category.categoryId,
                                editor.who() + " " + action + " category " + category.name);
    }

    public void noteAction (PlayerRecord editor, String action, Thing thing)
    {
        _adminRepo.recordAction(editor.userId, Action.Target.THING, thing.thingId,
                                editor.who() + " " + action + " thing " + thing.name);
    }

    public void noteDeleted (PlayerRecord editor, String what)
    {
        _adminRepo.recordAction(editor.userId, Action.Target.NONE, 0,
                                editor.who() + " deleted " + what);
    }

    @Inject protected AdminRepository _adminRepo;
}
