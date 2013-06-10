//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.tests;

import java.util.Collections;

import com.google.inject.Inject;

import com.threerings.everything.server.PlayerLogic;

/**
 * Tests sending Facebook reminder notifications.
 */
public class TestSendReminder extends TestBase
{
    public static void main (String[] args)
    {
        run(TestSendReminder.class, args);
    }

    public void run (String[] args)
    {
        if (args.length != 2) {
            usage("Usage: TestSendReminder facebookId idleDays", null);
            return;
        }

        long fbid = Long.parseLong(args[0]);
        int idleDays = Integer.parseInt(args[1]);
        _playerLogic.sendReminderNotifications(Collections.singleton(fbid), idleDays);
    }

    @Inject protected PlayerLogic _playerLogic;
}
