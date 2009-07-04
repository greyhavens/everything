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
import com.samskivert.util.Config;

import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.App;
import com.threerings.samsara.app.server.Binding;

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

    @Override // from App
    public String getIdentifier ()
    {
        return IDENT;
    }

    @Override // from App
    public Binding[] getBindings ()
    {
        List<Binding> binds = Lists.newArrayList();
        binds.add(new Binding.Servlet("/"+EverythingServlet.ENTRY_POINT, EverythingServlet.class));
        return binds.toArray(new Binding[binds.size()]);
    }

    @Override // from App
    public String getFacebookSecret (String uri)
    {
        return _config.getValue("facebook_secret", (String)null);
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
    }

    protected Config _config = new Config("everything", getClass().getClassLoader());

    @Inject protected @Named(AppCodes.APPVERS) String _appvers;
}
