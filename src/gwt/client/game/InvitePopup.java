//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.PopupPanel;

import com.threerings.everything.data.Card;

import client.util.Context;

/**
 * Displays an invitation to one friend (with a card gift) or multiple friends (with no gift) to
 * come and play Everything.
 */
public class InvitePopup extends PopupPanel
{
    public InvitePopup (Context ctx, Card card, Runnable onComplete)
    {
        addStyleName("inviteCard");
        _onComplete = onComplete;
        String uri = "showinvite";
        if (card != null) {
            uri += "?thing=" + card.thing.thingId;
            if (card.received != null) {
                uri += "&received=" + card.received.getTime();
            }
        }
        setWidget(new Frame(uri));
    }

    @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        registerCloseCallback(this);
    }

    @Override // from Widget
    protected void onUnload ()
    {
        super.onUnload();
        clearCloseCallback();
    }

    protected void onClose (boolean completed)
    {
        hide();
        if (completed && _onComplete != null) {
            _onComplete.run();
        }
    }

    protected native static void registerCloseCallback (InvitePopup popup) /*-{
        $wnd.closePopup = function (completed) {
            popup.@client.game.InvitePopup::onClose(Z)(completed);
        }
    }-*/;

    protected native static void clearCloseCallback () /*-{
        $wnd.closePopup = null;
    }-*/;

    protected Runnable _onComplete;
}
