//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.data;

import java.io.Serializable;
import java.util.Date;

/**
 * Contains data for a comment on a category.
 */
public class CategoryComment
    implements Serializable
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
