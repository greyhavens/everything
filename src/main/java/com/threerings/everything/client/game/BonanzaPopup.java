//
// $Id$

package com.threerings.everything.client.game;

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

import com.threerings.everything.rpc.GameService;
import com.threerings.everything.rpc.GameServiceAsync;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.SlotStatus;

import com.threerings.everything.client.ui.ButtonUI;
import com.threerings.everything.client.util.ClickCallback;
import com.threerings.everything.client.util.Context;

public class BonanzaPopup extends PopupPanel
{
    public BonanzaPopup (Context ctx, Card card, AsyncCallback<GameStatus> callback)
    {
        setStyleName("popup");
        _ctx = ctx;

        FluentTable box = new FluentTable(0, 0, "bonanza");
        box.add().alignCenter().setHTML("It's a card Bonanza!", "Title");
        box.add().setHTML(
            "You found a duplicate copy of this card! It wouldn't be right to keep it, but " +
            "you can earn an <b>extra free flip</b> if you post it to your feed. "+
            "Any of your friends that join the game by clicking on this post will start their " +
            "collections with this card!");

        FluentTable whiteBox = new FluentTable(0, 0, "attractorPreview");
        whiteBox.add().setColSpan(2).alignCenter().setHTML(card.thing.name);
        SlotView slot = new SlotView();
        slot.setStatus(SlotStatus.FLIPPED);
        slot.setCard(ctx, card.toThingCard(), false, new ClickHandler() {
            public void onClick (ClickEvent event) {
                // nada
            }
        });
        whiteBox.add().setWidget(slot).right().setText(card.thing.descrip);

        box.add().setWidget(whiteBox);
        box.add().alignCenter().setWidget(makeButtons(card, callback));

        setWidget(box);

        // now animate it in
        setVisible(false);
        _ctx.displayPopup(this, null);
        FX.move(this).from(-getOffsetWidth(), getAbsoluteTop()).run(500);
    }

    protected Widget makeButtons (final Card card, final AsyncCallback<GameStatus> callback)
    {
        PushButton post = ButtonUI.newButton("Post", new ClickHandler() {
            public void onClick (ClickEvent event) {
                ThingDialog.showAttractor(_ctx, card, new AsyncCallback<Boolean>() {
                    public void onSuccess (Boolean posted) {
                        if (!posted) {
                            return; // just leave the dialog up
                        }
                        _gamesvc.bonanzaViewed(card.thing.thingId,
                            new AsyncCallback<GameStatus>() {
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
                _gamesvc.bonanzaViewed(-1, this);
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
