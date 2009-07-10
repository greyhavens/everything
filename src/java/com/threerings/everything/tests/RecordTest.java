//
// $Id$

package com.threerings.everything.tests;

import org.junit.*;
import static org.junit.Assert.*;

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
        CardRecord card = new CardRecord();
        CategoryRecord category = new CategoryRecord();
        FlippedRecord flipped = new FlippedRecord();
        GridRecord grid = new GridRecord();
        PlayerRecord player = new PlayerRecord();
        PowerupRecord powerup = new PowerupRecord();
        ThingRecord thing = new ThingRecord();
        WishRecord wish = new WishRecord();
    }
}
