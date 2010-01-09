//
// $Id$

package client.ui;

import com.google.common.base.Function;

import com.google.gwt.core.client.GWT;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;

import client.ui.like.LikeImages;

public class LikeWidget extends HorizontalPanel
    implements Value.Listener<Boolean>
{
    public LikeWidget (int categoryId, Value<Boolean> liked)
    {
        _categoryId = categoryId;
        _liked = liked;

        add(_good = createImage(Boolean.TRUE));
        add(_neutral = createImage(null));
        add(_bad = createImage(Boolean.FALSE));
    }

    protected void onAttach ()
    {
        super.onAttach();
        _liked.addListenerAndTrigger(this);
    }

    protected void onDetach ()
    {
        super.onDetach();
        _liked.removeListener(this);
    }

    // from ValueListener
    public void valueChanged (Boolean liked)
    {
        ((liked == Boolean.TRUE) ? _images.pos_selected() : _images.pos()).applyTo(_good);
        ((liked == null) ? _images.neu_selected() : _images.neu()).applyTo(_neutral);
        ((liked == Boolean.FALSE) ? _images.neg_selected() : _images.neg()).applyTo(_bad);
    }

    protected Image createImage (final Boolean buttonValue)
    {
        Image img = new Image();
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

    protected Image _good, _neutral, _bad;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
    protected static final LikeImages _images = GWT.create(LikeImages.class);
}
