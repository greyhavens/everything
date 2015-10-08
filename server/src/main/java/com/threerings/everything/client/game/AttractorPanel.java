//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.client.game;

import com.google.gwt.core.client.GWT;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.rpc.GameService;
import com.threerings.everything.rpc.GameServiceAsync;
import com.threerings.everything.data.SlotStatus;

import com.threerings.everything.client.ui.DataPanel;
import com.threerings.everything.client.util.Context;

/**
 * A panel, placed on the landing page, for acquiring an 'attractor' card for your new collection.
 */
public class AttractorPanel extends DataPanel<GameService.CardResult>
{
    public AttractorPanel (Context ctx, int attractorId, int friendId)
    {
        super(ctx, "attractor");

        // grant ourselves this attractor, unless we already have one
        _gamesvc.getAttractor(attractorId, friendId, createCallback());
    }

    protected void init (GameService.CardResult data)
    {
        add(Widgets.newLabel("Congratulations. Your collection has been started!", "Title"));
        SlotView slot = new SlotView();
        slot.status.update(SlotStatus.FLIPPED);
        slot.setCard(_ctx, data.card.toThingCard(), false, null);
        add(slot);
    }

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
