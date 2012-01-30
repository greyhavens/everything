//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.tests;

import org.junit.*;
import static org.junit.Assert.*;

import com.threerings.everything.data.Rarity;

/**
 * Tests some rarity bits.
 */
public class RarityTest
{
    @Test public void testWeight ()
    {
        for (Rarity rarity : Rarity.values()) {
            assertTrue(rarity.weight() > 0);
        }
    }
}
