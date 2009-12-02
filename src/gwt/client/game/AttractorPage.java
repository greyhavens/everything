//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.SlotStatus;

import client.ui.DataPanel;
import client.util.Context;
import client.util.Page;

/**
 * A landing page for acquiring an 'attractor' card for your collection.
 */
public class AttractorPage extends DataPanel<GameService.CardResult>
{
    public AttractorPage (Context ctx, int attractorId, int friendId)
    {
        super(ctx, "attractor", "page"); // styles TODO

        if (ctx.getMe().isGuest()) {
            // How did they get here???
            add(Widgets.newHTML(
                ctx.getFacebookAddLink("Play everything and get the card you want",
                    Page.ATTRACTOR, attractorId, friendId)));
            return;
        }

        // grant ourselves this attractor, unless we already have one
        _gamesvc.getAttractor(attractorId, friendId, createCallback());
    }

    protected void init (GameService.CardResult data)
    {
        if (data == null) {
            // we've already got one!
            add(Widgets.newLabel("You've already got one!"));

        } else {

            add(Widgets.newLabel("Congratulations. You've been given this card. Enjoy"));
            SlotView slot = new SlotView();
            slot.status.update(SlotStatus.FLIPPED);
            slot.setCard(_ctx, data.card.toThingCard(), false, null);
            add(slot);
        }

        // TODO: link to news page? Or, for newbies, a link to "how to play"?
    }

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
