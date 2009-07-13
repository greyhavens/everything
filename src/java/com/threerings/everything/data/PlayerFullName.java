//
// $Id$

package com.threerings.everything.data;

/**
 * Extends {@link PlayerName} and includes the player's last name. Only shown to admins.
 */
public class PlayerFullName extends PlayerName
{
    /** This player's surname. */
    public String surname;
}
