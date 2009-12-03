//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.News;
import com.threerings.everything.data.SlotStatus;

import client.ui.DataPanel;
import client.util.Context;
import client.util.Page;

/**
 * A panel, placed on the landing page, for acquiring an 'attractor' card for your new collection.
 */
public class AttractorPanel extends DataPanel<GameService.CardResult>
{
    public AttractorPanel (Context ctx, int attractorId, int friendId)
    {
        super(ctx, "attractor"); // styles TODO

        // grant ourselves this attractor, unless we already have one
        _gamesvc.getAttractor(attractorId, friendId, createCallback());
    }

    protected void init (GameService.CardResult data)
    {
        if (data == null) {
            // they're either an old player or already have the card
            add(Widgets.newLabel("Sorry, this card is only available for new players"));

        } else {
            add(Widgets.newLabel("Congratulations. Your collection has been started!"));
            SlotView slot = new SlotView();
            slot.status.update(SlotStatus.FLIPPED);
            slot.setCard(_ctx, data.card.toThingCard(), false, null);
            add(slot);
        }
    }

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
