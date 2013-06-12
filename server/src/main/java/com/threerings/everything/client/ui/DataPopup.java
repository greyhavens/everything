//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InfoPopup;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.util.Context;

/**
 * A base class for popups that display a "Loading..." indicator while loading some data from the
 * server and then display the data.
 */
public abstract class DataPopup<T> extends PopupPanel
{
    protected DataPopup (String styleName, Context ctx)
    {
        setStyleName("popup");
        addStyleName(styleName);
        setWidget(Widgets.newLabel("Loading...", "infoLabel"));
        _ctx = ctx;
    }

    /**
     * This is called by the callback when the data has been loaded.
     */
    protected abstract Widget createContents (T data);

    /**
     * Creates a callback that should be passed to the service method that obtains the data.
     */
    protected AsyncCallback<T> createCallback ()
    {
        return new AsyncCallback<T>() {
            public void onSuccess (T data) {
                recontent(createContents(data));
            }
            public void onFailure (Throwable t) {
                recontent(Widgets.newLabel(t.getMessage(), "errorLabel"));
                setAutoHideEnabled(true);
                new Timer() {
                    public void run () {
                        hide();
                    }
                }.schedule(InfoPopup.computeAutoClearDelay(t.getMessage()));
            }
        };
    }

    /**
     * Creates a {@link ClickHandler} that will hide this popup when activated.
     */
    protected ClickHandler onHide ()
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                DataPopup.this.hide();
            }
        };
    }

    protected void recontent (Widget content)
    {
        int ypos = getAbsoluteTop() + getOffsetHeight()/2;
        setWidget(content);
        Popups.centerOn(this, ypos);
    }

    protected Context _ctx;
}
