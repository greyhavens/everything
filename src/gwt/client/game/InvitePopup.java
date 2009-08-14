//
// $Id$

package client.game;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;

import com.threerings.everything.client.Kontagent;
import com.threerings.everything.data.Card;

import client.ui.XFBML;
import client.util.Context;
import client.util.KontagentUtil;
import client.util.Page;

/**
 * Displays an invitation to one friend (with a card gift) or multiple friends (with no gift) to
 * come and play Everything.
 */
public class InvitePopup extends PopupPanel
{
    public InvitePopup (Context ctx, Card card)
    {
        addStyleName("inviteCard");

        String tracking = KontagentUtil.generateUniqueId(ctx.getMe().userId);
        String title, button, action;
        int maxInvites;

        if (card != null) {
            title = ctx.getMe().name + " wants you to have the <b>" + card.thing.name +
                "</b> card in The Everything Game.";
            button = "View the card!";
            action = "Who do you want to give the " + card.thing.name + " card to?";
            maxInvites = 1;

        } else {
            title = ctx.getMe().name + " wants you to play The Everything Game with them.";
            button = "Play Everything";
            action = "Who do you want to invite to play The Everything Game?";
            maxInvites = 4;
        }

        String url = ctx.getFacebookAddURL(Kontagent.INVITE, tracking, Page.LANDING);
        String content = title + "<fb:req-choice url='" + url + "' label='" + button + "' />";
        FlowPanel contents = XFBML.newPanel("request-form", "action", getNoteInviteURL(),
                                            "method", "POST", "invite", "true",
                                            "type", "Everything Game", "content", content);
        FlowPanel wrap = new FlowPanel();
        DOM.setStyleAttribute(wrap.getElement(), "width", "100%");
        DOM.setStyleAttribute(wrap.getElement(), "padding", "0px 50px");
        DOM.setStyleAttribute(wrap.getElement(), "background", "#D2D9E6");
        wrap.add(XFBML.newTag("multi-friend-selector", "actiontext", action, "max", ""+maxInvites,
                              "email_invite", "false", "cols", "3", "rows", "3",
                              "showborder", "true"));
        contents.add(wrap);
        if (card != null) {
            contents.add(XFBML.newHiddenInput("thing", ""+card.thing.thingId));
            contents.add(XFBML.newHiddenInput("received", ""+card.received.getTime()));
        }
        contents.add(XFBML.newHiddenInput("tracking", tracking));
        contents.add(XFBML.newHiddenInput("from", History.getToken()));

        setWidget(XFBML.serverize(contents, "style", "width: 586px; min-height: 400px"));
    }

    protected static String getNoteInviteURL ()
    {
        String url = Window.Location.getHref();
        int eidx = url.indexOf("/everything");
        return url.substring(0, eidx + "/everything".length()) + "/invite";
    }
}
