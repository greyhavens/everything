//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains data for a comment on a category.
 */
public class CategoryComment
    implements IsSerializable
{
    /** The category on which the comment was made. */
    public int categoryId;

    /** The time at which the action was taken. */
    public Date when;

    /** The commentor. */
    public PlayerName commentor;

    /** The text of the comment. */
    public String message;
}
