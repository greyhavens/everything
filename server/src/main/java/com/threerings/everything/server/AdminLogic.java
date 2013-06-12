//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.everything.data.Action;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.PlayerName;
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
                                action + " category " + category.name);
    }

    public void noteAction (PlayerRecord editor, String action, Thing thing)
    {
        _adminRepo.recordAction(editor.userId, Action.Target.THING, thing.thingId,
                                action + " thing " + thing.name);
    }

    public void noteAction (int adminId, String action, PlayerName player)
    {
        _adminRepo.recordAction(adminId, Action.Target.PLAYER, player.userId,
                                action + " player " + player.name);
    }

    public void noteAction (int adminId, String action)
    {
        _adminRepo.recordAction(adminId, Action.Target.NONE, 0, action);
    }

    @Inject protected AdminRepository _adminRepo;
}
