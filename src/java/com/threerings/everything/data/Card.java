//
// $Id$

package com.threerings.everything.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Provides data for a card to the client.
 */
public class Card
    implements IsSerializable
{
    /** The player that owns this card. */
    public PlayerName owner;

    /** The thing that's on this card. */
    public Thing thing;

    /** The time at which this card was created. */
    public Date created;

    /** The player that gave this card to the owner or null. */
    public PlayerName giver;
}
