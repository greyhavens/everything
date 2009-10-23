//
// $Id$

package com.threerings.everything.tests;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.name.Names;

import org.easymock.EasyMock;

import com.samskivert.depot.PersistenceContext;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.common.ServletLogic;
import com.threerings.samsara.common.UserLogic;

import com.threerings.everything.server.EverythingApp;

/**
 * A base class for tests that need to be run on the command line and access a database.
 */
public abstract class TestBase
{
    public abstract void run (String[] args);

    public static void run (Class<? extends TestBase> testClass, String[] args)
    {
        Injector injector = Guice.createInjector(new TestModule());
        EverythingApp app = injector.getInstance(EverythingApp.class);
        app.willAttach();
        app.didAttach();
        injector.getInstance(testClass).run(args);
        app.didDetach();
    }

    protected static void usage (String usage, String errmsg)
    {
        System.err.println("Usage: " + usage);
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
        @Provides protected UserLogic getUserLogic () {
            return EasyMock.createMock(UserLogic.class);
        }
        @Provides protected ServletLogic getServletLogic () {
            return EasyMock.createMock(ServletLogic.class);
        }
    }
}
