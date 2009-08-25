//
// $Id$

package com.threerings.everything.tests;

import org.junit.*;

import com.threerings.everything.server.persist.*;

/**
 * Tests that the conversion functions on all of our persistent records can be created.
 */
public class RecordTest
{
    @Test public void testInstantiate ()
    {
        // this will catch whether we're missing any magical methods that are needed for
        // RuntimeUtil.makeToRuntime
        new CardRecord();
        new CategoryRecord();
        new GridRecord();
        new PlayerRecord();
        new PowerupRecord();
        new ThingRecord();
        new WishRecord();
    }
}
