//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains a news blurb.
 */
public class News
    implements IsSerializable
{
    /** The maximum length of a news post. */
    public static final int MAX_NEWS_LENGTH = 4096;

    /** The time at which this news was reported. */
    public Date reported;

    /** The admin that reported the news. */
    public PlayerName reporter;

    /** The text of the news posting. */
    public String text;
}
