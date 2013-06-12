//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.game;

import com.google.gwt.user.client.ui.Label;

import com.threerings.everything.data.Rarity;

/**
 * Displays a rarity along with a tooltip with more detailed info.
 */
public class RarityLabel extends Label
{
    public RarityLabel (Rarity rarity)
    {
        this("", rarity);
    }

    public RarityLabel (String label, Rarity rarity)
    {
        setStyleName("Rarity");
        addStyleName("inline");
        addStyleName("machine");
        if (rarity == null) {
            setText(label + "?");
        } else {
            setText(label + rarity.toString());
            setTitle(rarity.description());
        }
    }
}
