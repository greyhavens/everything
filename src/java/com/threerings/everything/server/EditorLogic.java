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
 * Provides editor services to server entities.
 */
@Singleton
public class EditorLogic
{
    public void noteAction (PlayerRecord editor, String action, Category category)
    {
        _adminRepo.recordAction(editor.userId, Action.Target.CATEGORY, category.categoryId,
                                action + " category " + category.name);
    }

    public void noteAction (PlayerRecord editor, String action, Thing thing)
    {
        _adminRepo.recordAction(editor.userId, Action.Target.THING, thing.thingId,
                                action + " thing " + thing.name);
    }

    @Inject protected AdminRepository _adminRepo;
}
