//
// $Id$

package com.threerings.everything.tests;

import com.google.inject.Guice;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.name.Names;

import org.easymock.EasyMock;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.StringUtil;

import com.threerings.samsara.app.client.ServiceException;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.AppLogic;
import com.threerings.samsara.app.server.ServletLogic;
import com.threerings.samsara.app.server.UserLogic;

import com.threerings.everything.data.Powerup;
import com.threerings.everything.server.EverythingApp;
import com.threerings.everything.server.GameLogic;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;

import static com.threerings.everything.Log.log;

/**
 * A standalone tool for testing grid generation. Needs a database connection and player id and
 * therefore isn't set up as a simple unit test.
 */
public class TestSelectGridThings
{
    public static void main (String[] args)
    {
        int userId;
        Powerup pup;
        try {
            userId = Integer.parseInt(args[0]);
        } catch (Exception e) {
            usage(null);
            return;
        }
        try {
            pup = (args.length > 1) ? Enum.valueOf(Powerup.class, args[1].toUpperCase()) : null;
        } catch (Exception e) {
            usage("Valid powerups: " + StringUtil.toString(Powerup.values()));
            return;
        }

        Injector injector = Guice.createInjector(new TestModule());
        EverythingApp app = injector.getInstance(EverythingApp.class);
        ConnectionProvider conprov = app.createConnectionProvider(app.getIdentifier());
        PersistenceContext perCtx = injector.getInstance(PersistenceContext.class);
        perCtx.init(app.getIdentifier(), conprov, null);
        perCtx.initializeRepositories(true);

        injector.getInstance(TestSelectGridThings.class).run(userId, pup);
    }

    public void run (int userId, Powerup pup)
    {
        PlayerRecord player = _playerRepo.loadPlayer(userId);
        if (player == null) {
            log.warning("Unknown player", "id", userId);
            return;
        }

        log.info("Selecting grid", "for", player.who(), "pup", pup);
        try {
            log.info("Selected grid", "grid", _gameLogic.selectGridThings(player, pup));
        } catch (ServiceException se) {
            log.warning("Failed to select grid", "cause", se.getMessage());
        }
    }

    protected static void usage (String errmsg)
    {
        System.err.println("Usage: TestSelectGridThings userId [powerup]");
        if (errmsg != null) {
            System.err.println(errmsg);
        }
        System.exit(255);
    }

    protected static class TestModule extends AbstractModule
    {
        protected void configure() {
            bind(String.class).annotatedWith(Names.named(AppCodes.APPVERS)).
                toInstance(AppCodes.RELEASE_CANDIDATE);
            bind(PersistenceContext.class).toInstance(new PersistenceContext());
        }
        @Provides protected AppLogic getAppLogic () {
            return EasyMock.createMock(AppLogic.class);
        }
        @Provides protected UserLogic getUserLogic () {
            return EasyMock.createMock(UserLogic.class);
        }
        @Provides protected ServletLogic getServletLogic () {
            return EasyMock.createMock(ServletLogic.class);
        }
    }

    @Inject protected GameLogic _gameLogic;
    @Inject protected PlayerRepository _playerRepo;
}
