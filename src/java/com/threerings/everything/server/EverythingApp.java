//
// $Id$

package com.threerings.everything.server;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.samskivert.io.StreamUtil;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.Config;
import com.threerings.user.OOOUser;
import com.threerings.util.PostgresUtil;

import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.App;
import com.threerings.samsara.app.server.Binding;

import com.threerings.everything.data.Build;
import com.threerings.everything.server.persist.AdminRepository;
import com.threerings.everything.server.persist.GameRepository;
import com.threerings.everything.server.persist.PlayerRepository;
import com.threerings.everything.server.persist.ThingRepository;

import static com.threerings.everything.Log.log;

/**
 * The main entry point for the Everything app.
 */
@Singleton
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
     * Returns an executor that can be used for background processing tasks.
     */
    public Executor getExecutor ()
    {
        return _executor;
    }

    /**
     * Returns our Facebook API key.
     */
    public String getFacebookKey ()
    {
        return _config.getValue(getFacebookKey("facebook_key"), (String)null);
    }

    /**
     * Returns our Facebook app secret.
     */
    public String getFacebookSecret ()
    {
        return _config.getValue(getFacebookKey("facebook_secret"), (String)null);
    }

    /**
     * Returns the URL we can send people to do access our Facebook app.
     */
    public String getFacebookAppURL (Object... args)
    {
        StringBuilder token = new StringBuilder();
        for (Object arg : args) {
            token.append((token.length() == 0) ? "?token=" : "~");
            token.append(arg);
        }
        String appname = _config.getValue(getFacebookKey("facebook_appname"), "missing_appname");
        return "http://apps.facebook.com/" + appname + "/" + token;
    }

    /**
     * Returns the id of our S3 media store.
     */
    public String getMediaStoreId ()
    {
        return _config.getValue("mediastore_id", (String)null);
    }

    /**
     * Returns the secret key to our S3 media store.
     */
    public String getMediaStoreKey ()
    {
        return _config.getValue("mediastore_key", (String)null);
    }

    /**
     * Returns the S3 bucket to which to upload when saving to our media store.
     */
    public String getMediaStoreBucket ()
    {
        return _config.getValue("mediastore_bucket", (String)null);
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
        binds.add(new Binding.Servlet("/invite", InviteServlet.class));
        binds.add(new Binding.Servlet("/upload", MediaUploadServlet.class));
        binds.add(new Binding.Servlet("/"+EverythingServlet.ENTRY_POINT, EverythingServlet.class));
        binds.add(new Binding.Servlet("/"+GameServlet.ENTRY_POINT, GameServlet.class));
        binds.add(new Binding.Servlet("/"+EditorServlet.ENTRY_POINT, EditorServlet.class));
        binds.add(new Binding.Servlet("/"+AdminServlet.ENTRY_POINT, AdminServlet.class));
//         binds.add(Binding.Job.every(1, new Runnable() {
//             public void run () {
//                 _gameLogic.processBirthdays();
//             }
//         }));
        return binds.toArray(new Binding[binds.size()]);
    }

    @Override // from App
    public int getSiteId ()
    {
        return OOOUser.EVERYTHING_SITE_ID;
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
        log.info("Everything app initialized.", "version", _appvers, "build", Build.version());
    }

    @Override // from App
    public void didDetach ()
    {
        log.info("Everything app detached.", "version", _appvers);
        // shut down our executors
        _executor.shutdown();
        // TODO: we want to wait for all of our pending servlets to finish before shutdown
        shutdown();
    }

    protected String getFacebookKey (String key)
    {
        return (_appvers.equals(AppCodes.RELEASE_CANDIDATE) ? "candidate_" : "") + key;
    }

    protected Config _config = createConfig("everything");
    protected ExecutorService _executor = Executors.newFixedThreadPool(3);

    @Inject protected @Named(AppCodes.APPVERS) String _appvers;
    @Inject protected GameLogic _gameLogic;

    // we need to inject all repositories here to ensure that they are resolved when our app is
    // resolved so that everything is registered and ready to go when Samsara initializes Depot
    @Inject protected AdminRepository _adminRepo;
    @Inject protected GameRepository _gameRepo;
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected ThingRepository _thingRepo;
}
