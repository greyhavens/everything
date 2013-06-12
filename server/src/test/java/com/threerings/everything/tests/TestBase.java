//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.tests;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import org.easymock.EasyMock;

import com.samskivert.depot.PersistenceContext;
import com.threerings.app.server.ServletLogic;
import com.threerings.app.server.UserLogic;

/**
 * A base class for tests that need to be run on the command line and access a database.
 */
public abstract class TestBase
{
    public abstract void run (String[] args);

    public static void run (Class<? extends TestBase> testClass, String[] args)
    {
        Injector injector = Guice.createInjector(new TestModule());
        injector.getInstance(testClass).run(args);
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
