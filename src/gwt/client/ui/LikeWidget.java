//
// $Id$

package client.ui;

import com.google.common.base.Function;

import com.google.gwt.core.client.GWT;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;

import client.ui.like.LikeImages;
import client.util.Context;

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
     * Construct a LikeWidget that displays the user's like preference and allows it to
     * be edited.
     */
    public LikeWidget (Context ctx, int categoryId)
    {
        _categoryId = categoryId;
        _liked = ctx.getLike(categoryId);

        add(_pos = createImage(Boolean.TRUE));
        add(_neg = createImage(Boolean.FALSE));
    }

    // from ValueListener
    public void valueChanged (Boolean liked)
    {
        proto(Boolean.TRUE, liked).applyTo(_pos);
        proto(Boolean.FALSE, liked).applyTo(_neg);
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
                updateLike(buttonValue);
            }
        }, _liked.map(new Function<Boolean, Boolean>() {
            public Boolean apply (Boolean liked) {
                return !_disarmed && (liked != buttonValue);
            }
        }));
        return img;
    }

    protected AbstractImagePrototype proto (Boolean buttonValue, Boolean liked)
    {
        return buttonValue
            ? ((liked == buttonValue) ? _images.pos_selected() : _images.pos())
            : ((liked == buttonValue) ? _images.neg_selected() : _images.neg());
    }

    protected void updateLike (final Boolean like)
    {
        _disarmed = true;
        // jiggle liked to disable our buttons
        _liked.update(_liked.get());

        _gamesvc.setLike(_categoryId, like, new AsyncCallback<Void>() {
            public void onFailure (Throwable err) {
                // nada
            }

            public void onSuccess (Void result) {
                _disarmed = false;
                _liked.update(like);
            }
        });
    }

    protected int _categoryId;
    protected boolean _disarmed;
    protected Value<Boolean> _liked;

    protected Image _pos, _neg;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
    protected static final LikeImages _images = GWT.create(LikeImages.class);
}
