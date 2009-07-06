//
// $Id$

package com.threerings.everything.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.Config;
import com.threerings.util.PostgresUtil;

import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.App;
import com.threerings.samsara.app.server.Binding;

import com.threerings.everything.server.persist.PlayerRepository;
import com.threerings.everything.server.persist.ThingRepository;

import static com.threerings.everything.Log.log;

/**
 * The main entry point for the Everything app.
 */
public class EverythingApp extends App
{
    /** Our app identifier. */
    public static final String IDENT = "everything";

    public static class Module extends AbstractModule
    {
        @Override protected void configure () {
            bind(App.class).to(EverythingApp.class);
        }
    }

    /**
     * Returns our Facebook API key.
     */
    public String getFacebookKey ()
    {
        return _config.getValue("facebook_key", (String)null);
    }

    /**
     * Returns our Facebook app secret.
     */
    public String getFacebookSecret ()
    {
        return _config.getValue("facebook_secret", (String)null);
    }

    @Override // from App
    public String getIdentifier ()
    {
        return IDENT;
    }

    @Override // from App
    public Binding[] getBindings ()
    {
        List<Binding> binds = Lists.newArrayList();
        binds.add(new Binding.Servlet("/auth", AuthServlet.class));
        binds.add(new Binding.Servlet("/"+EverythingServlet.ENTRY_POINT, EverythingServlet.class));
        binds.add(new Binding.Servlet("/"+GameServlet.ENTRY_POINT, GameServlet.class));
        binds.add(new Binding.Servlet("/"+AdminServlet.ENTRY_POINT, AdminServlet.class));
        return binds.toArray(new Binding[binds.size()]);
    }

    @Override // from App
    public String getFacebookSecret (String uri)
    {
        return getFacebookSecret(); // we don't do per-uri secrets
    }

    @Override // from App
    public ConnectionProvider createConnectionProvider (String ident)
    {
        return PostgresUtil.createPoolingProvider(_config, ident);
    }

    @Override // from App
    public void didInit ()
    {
        log.info("Everything app initialized.", "version", _appvers);
    }

    @Override // from App
    public void didDetach ()
    {
        log.info("Everything app detached.", "version", _appvers);
        // TODO: we want to wait for all of our pending servlets to finish before shutdown
        shutdown();
    }

    protected Config _config = new Config("everything", getClass().getClassLoader());

    @Inject protected @Named(AppCodes.APPVERS) String _appvers;

    // we need to inject all repositories here to ensure that they are resolved when our app is
    // resolved so that everything is registered and ready to go when Samsara initializes Depot
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected ThingRepository _thingRepo;
}
