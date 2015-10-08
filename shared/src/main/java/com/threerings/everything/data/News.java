//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.data;

import java.io.Serializable;
import java.util.Date;

/**
 * Contains a news blurb.
 */
public class News
    implements Serializable
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
