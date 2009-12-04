//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FX;
import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.SlotStatus;

import client.ui.ButtonUI;
import client.util.ClickCallback;
import client.util.Context;

public class BonanzaPopup extends PopupPanel
{
    public BonanzaPopup (
        Context ctx, GameService.BonanzaInfo bonanzaInfo, AsyncCallback<GameStatus> callback)
    {
        setStyleName("popup");
        _ctx = ctx;

        FluentTable box = new FluentTable(0, 0, "bonanza");
        box.add().setColSpan(2).alignCenter().setHTML("It's a card Bonanza!");
        box.add().setColSpan(2).setHTML(
            "You found an extra card while flipping! Now, it just wouldn't be fair for you " +
            "to keep it, but you can earn an extra free flip if you post it to your feed.");

        box.add().setColSpan(2).alignCenter().setHTML(bonanzaInfo.title);
        SlotView slot = new SlotView();
        slot.setStatus(SlotStatus.FLIPPED);
        slot.setCard(ctx, bonanzaInfo.card.toThingCard(), false, new ClickHandler() {
            public void onClick (ClickEvent event) {
                // nada
                // TODO?
            }
        });
        box.add().setWidget(slot).right().setHTML(bonanzaInfo.message);


        box.add().setColSpan(2).alignCenter().setWidget(makeButtons(bonanzaInfo, callback));

        setWidget(box);

        // now animate it in
        setVisible(false);
        _ctx.displayPopup(this, null);
        FX.move(this).from(-getOffsetWidth(), getAbsoluteTop()).run(500);
    }

    protected Widget makeButtons (
        final GameService.BonanzaInfo bonanzaInfo, final AsyncCallback<GameStatus> callback)
    {
        PushButton post = ButtonUI.newButton("Post", new ClickHandler() {
            public void onClick (ClickEvent event) {
                ThingDialog.showAttractor(_ctx, bonanzaInfo.card, bonanzaInfo.title,
                    bonanzaInfo.message, new AsyncCallback<Boolean>() {
                        public void onSuccess (Boolean posted) {
                            if (!posted) {
                                return; // just leave the dialog up
                            }
                            _gamesvc.bonanzaViewed(true, new AsyncCallback<GameStatus>() {
                                public void onSuccess (GameStatus status) {
                                    callback.onSuccess(status);
                                }

                                public void onFailure (Throwable cause) {
                                    // TODO (shouldn't happen)
                                }
                            });
                            onHide().onClick(null);
                        }
                        public void onFailure (Throwable cause) {
                            // TODO (shouldn't happen)
                        }
                    });
            }
        });
        PushButton cancel = ButtonUI.newButton("Skip", onHide());
        new ClickCallback<GameStatus>(cancel) {
            protected boolean callService () {
                _gamesvc.bonanzaViewed(false, this);
                return true;
            }
            protected boolean gotResult (GameStatus status) {
                // status == null, don't do shit.. right?
                return true;
            }
        };

        return Widgets.newRow(cancel, post);
    }

    protected ClickHandler onHide ()
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                int tx = -getOffsetWidth(), ty = getAbsoluteTop();
                FX.move(BonanzaPopup.this).to(tx, ty).onComplete(new Command() {
                    public void execute () {
                        hide();
                    }
                }).run(500);
            }
        };
    }

    protected Context _ctx;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
