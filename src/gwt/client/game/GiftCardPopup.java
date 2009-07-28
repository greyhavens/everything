//
// $Id$

package client.game;

import java.util.Collections;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.DefaultTextListener;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.FriendCardInfo;
import com.threerings.everything.data.Thing;

import client.ui.ButtonUI;
import client.ui.DataPopup;
import client.ui.XFBML;
import client.util.Args;
import client.util.ClickCallback;
import client.util.Context;
import client.util.Page;

/**
 * Displays information on which of a player's friends has a particular card and allows them to
 * gift the card to a friend.
 */
public class GiftCardPopup extends DataPopup<GameService.GiftInfoResult>
{
    public static ClickHandler onClick (final Context ctx, final Card card, final Runnable onGifted)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                ctx.displayPopup(new GiftCardPopup(ctx, card.thing, card.created.getTime(),
                                                   onGifted));
            }
        };
    }

    public GiftCardPopup (Context ctx, Thing thing, long created, Runnable onGifted)
    {
        super("giftCard", ctx);
        _thing = thing;
        _created = created;
        _onGifted = onGifted;
        _gamesvc.getGiftCardInfo(thing.thingId, created, createCallback());
    }

    @Override // from DataPopup<GameService.GiftInfoResult>
    protected Widget createContents (GameService.GiftInfoResult result)
    {
        Collections.sort(result.friends);

        SmartTable grid = new SmartTable(5, 0);
        grid.setWidth("100%");
        int row = 0, col = 0;
        for (final FriendCardInfo info : result.friends) {
            String text = info.friend.toString();
            if (info.onWishlist) {
                text += " (on wishlist)";
            } else if (info.hasThings > 0) {
                text += " (has " + info.hasThings + "/" + result.things + ")";
            }
            grid.setText(row, col, text, 1, "nowrap");
            final TextBox message = Widgets.newTextBox("", 255, 20);
            final String defmsg = "<optional gift message>";
            DefaultTextListener.configure(message, defmsg);
            message.setVisible(false);
            grid.setWidget(row, col+1, message);
            final PushButton give = ButtonUI.newSmallButton("Give");
            grid.setWidget(row, col+2, give);
            new ClickCallback<Void>(give, message) {
                protected boolean callService () {
                    if (!message.isVisible()) {
                        message.setVisible(true);
                        center(); // recenter the popup
                        give.setText("Send");
                        return false;
                    }
                    String msg = DefaultTextListener.getText(message, defmsg);
                    _gamesvc.giftCard(_thing.thingId, _created, info.friend.userId, msg, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    GiftCardPopup.this.hide();
                    _onGifted.run();
                    Popups.info("Card gifted. Your friend will be so happy!");
                    return false;
                }
            };
            col += 3;
            if (col > 5) {
                row++;
                col = 0;
            }
        }
        String msg = (result.friends.size() == 0) ?
            "All of your Everything friends already have this card." :
            "Friends that already have this card are not shown.";
        grid.addText(msg, 6);

        return Widgets.newFlowPanel(
            Widgets.newLabel("Send it to an Everything friend:", "machine"),
            Widgets.newScrollPanelY(grid, 400),
            Widgets.newShim(10, 10),
            Widgets.newRow(Widgets.newLabel("Send it to a Facebook friend:", "machine"),
                           ButtonUI.newSmallButton("Pick", new ClickHandler() {
                               public void onClick (ClickEvent event) {
                                   GiftCardPopup.this.hide();
                                   _ctx.displayPopup(makeInvitePopup());
                               }
                           })),
            Widgets.newFlowPanel("Buttons", ButtonUI.newButton("Cancel", onHide())));
    }

    protected PopupPanel makeInvitePopup ()
    {
        String url = _ctx.getFacebookAddURL() + "*token=" +
            Args.createLinkToken(Page.BROWSE, "", _thing.categoryId);
        String content = _ctx.getMe().name + " wants you to have the <b>" + _thing.name +
            "</b> card in The Everything Game." +
            "<fb:req-choice url='" + url + "' label='View the card!' />";
        FlowPanel other = XFBML.newPanel("request-form", "action", getInviteURL(), "method", "POST",
                                         "invite", "true", "type", "Everything Game",
                                         "content", content);
        FlowPanel wrap = new FlowPanel();
        DOM.setStyleAttribute(wrap.getElement(), "margin", "5px");
        wrap.add(XFBML.newTag("friend-selector"));
        wrap.add(XFBML.newTag("request-form-submit"));
        other.add(wrap);
        other.add(XFBML.newHiddenInput("thing", ""+_thing.thingId));
        other.add(XFBML.newHiddenInput("created", ""+_created));
        other.add(XFBML.newHiddenInput("from", History.getToken()));

        FlowPanel contents = Widgets.newFlowPanel(
            Widgets.newLabel("Send this card to a Facebook friend:", "Title", "machine"),
            XFBML.serverize(other, "style", "min-height: 400px"));
        PopupPanel popup = Popups.newPopup("inviteCard", contents);
        popup.addStyleName("popup");
        contents.add(Widgets.newFlowPanel(
                         "Buttons", ButtonUI.newButton("Cancel", Popups.createHider(popup))));
        return popup;
    }

    protected static String getInviteURL ()
    {
        String url = Window.Location.getHref();
        int eidx = url.indexOf("/everything");
        return url.substring(0, eidx + "/everything".length()) + "/invite";
    }

    protected Thing _thing;
    protected long _created;
    protected Runnable _onGifted;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
