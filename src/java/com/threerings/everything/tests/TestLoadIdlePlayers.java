//
// $Id$

package com.threerings.everything.tests;

import com.google.inject.Inject;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;

/**
 * Tests {@link PlayerRepository#loadIdlePlayers}.
 */
public class TestLoadIdlePlayers extends TestBase
{
    public static void main (String[] args)
    {
        run(TestLoadIdlePlayers.class, args);
    }

    public void run (String[] args)
    {
        for (PlayerRecord prec : _playerRepo.loadIdlePlayers()) {
            System.out.println(prec);
        }
    }

    @Inject protected PlayerRepository _playerRepo;
}
