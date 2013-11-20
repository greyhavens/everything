//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.io.Serializable;

/**
 * A message delivered to a player when they start their session.
 */
public class Notice implements Serializable
{
    /** Enumerates the different kind of notices. */
    public static enum Kind { FRIEND_JOINED, PLAYED_MOBILE }

    /** The kind of notice being delivered. */
    public Kind kind;

    /** An optional string argument used when displaying the notice. */
    public String subject;

    /** An optional coins amount used when displaying the notice. */
    public int coins;

    /** Used when unserializing. */
    public Notice () {}

    public Notice (Kind kind, String subject, int coins) {
        this.kind = kind;
        this.subject = subject;
        this.coins = coins;
    }
}
