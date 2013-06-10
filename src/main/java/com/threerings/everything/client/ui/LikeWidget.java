//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.ui;

import com.google.gwt.core.client.GWT;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerManager;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

import com.threerings.everything.rpc.GameService;
import com.threerings.everything.rpc.GameServiceAsync;
import com.threerings.everything.data.Card;

import com.threerings.everything.client.ui.like.LikeImages;
import com.threerings.everything.client.util.Context;

public class LikeWidget extends HorizontalPanel
    implements Value.Listener<Boolean>
{
    /**
     * Get an Image that displays another user's uneditable like preference for some category.
     */
    public static Image getDisplay (Boolean like)
    {
        if (like == null) {
            return null;
        }
        Image img = (like ? _images.pos() : _images.neg()).createImage();
        img.setTitle(like ? "Liked" : "Disliked");
        return img;
    }

    /**
     * Return a LikeWidget ensconced in a buttony border.
     */
    public static Widget asFakeButton (Context ctx, Card card)
    {
        final AbsolutePanel pan = new AbsolutePanel();
        PushButton fake = ButtonUI.newButton("");
        fake.setEnabled(false);
        new HandlerManager(fake).addHandler(ResizeEvent.getType(), new ResizeHandler() {
            public void onResize (ResizeEvent event) {
                pan.setWidth(event.getWidth() + "px");
                pan.setHeight(event.getHeight() + "px");
            }
        });
        pan.add(fake);
        pan.add(new LikeWidget(ctx, card), 22, 12);
        return pan;
    }

    /**
     * Construct a LikeWidget that displays the user's like preference and allows it to
     * be edited.
     */
    public LikeWidget (Context ctx, int categoryId)
    {
        _categoryId = categoryId;
        _liked = ctx.getLike(categoryId);

        setSpacing(2);
        add(_pos = createImage(true));
        add(_neg = createImage(false));
    }

    public LikeWidget (Context ctx, Card card)
    {
        this(ctx, card.getSeries().categoryId);
    }

    // from ValueListener
    public void valueChanged (Boolean liked)
    {
        proto(true, liked).applyTo(_pos);
        proto(false, liked).applyTo(_neg);
    }

    protected void onLoad ()
    {
        super.onLoad();
        _liked.addListener(this);
    }

    protected void onUnload ()
    {
        _liked.removeListener(this);
        super.onUnload();
    }

    protected Image createImage (final Boolean buttonValue)
    {
        Image img = proto(buttonValue, _liked.get()).createImage();
        img.setTitle(buttonValue ? "I like this series" : "I dislike this series");
        Widgets.makeActionable(img, new ClickHandler() {
            public void onClick (ClickEvent event) {
                // clicking while selected deselects and returns to no pref
                updateLike((_liked.get() == buttonValue) ? null : buttonValue);
            }
        }, _armed);
        return img;
    }

    protected AbstractImagePrototype proto (Boolean buttonValue, Boolean liked)
    {
        return buttonValue
            ? ((liked == buttonValue) ? _images.pos_selected() : _images.pos())
            : ((liked == buttonValue) ? _images.neg_selected() : _images.neg());
    }

    protected void updateLike (final Boolean newLike)
    {
        // disable the button
        _armed.update(false);

        _gamesvc.setLike(_categoryId, newLike, new AsyncCallback<Void>() {
            public void onFailure (Throwable err) {
                // nada
            }

            public void onSuccess (Void result) {
                _liked.update(newLike);
                _armed.update(true);
            }
        });
    }

    protected int _categoryId;
    protected Value<Boolean> _liked;

    protected Image _pos, _neg;
    protected Value<Boolean> _armed = Value.create(true);

    protected static final LikeImages _images = GWT.create(LikeImages.class);
    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
