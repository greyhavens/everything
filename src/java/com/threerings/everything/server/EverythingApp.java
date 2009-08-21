//
// $Id$

package com.threerings.everything.server;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.Config;
import com.samskivert.util.StringUtil;
import com.threerings.user.OOOUser;
import com.threerings.util.PostgresUtil;

import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.App;
import com.threerings.samsara.app.server.Binding;

import com.threerings.everything.client.Kontagent;
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
     * Returns a URL to our Facebook app that we can put out in the wide world.
     *
     * @param type the vector in which this URL is being embedded, e.g. {@link Kontagent#INVITE}).
     * @param the tracking code reported to Kontagent when the invite or notification in which this
     * URL is being embedded was reported.
     */
    public String getHelloURL (Kontagent type, String tracking, Object... args)
    {
        String url = getFacebookAppURL() + "?kc=" + type.code + "&t=" + tracking;
        if (args.length > 0) {
            url += "&token=" + Joiner.on("~").join(args);
        }
        return url;
    }

    /**
     * Returns the bare URL to our Facebook app. You probably want {@link #getHelloURL}.
     */
    public String getFacebookAppURL ()
    {
        String appname = _config.getValue(getFacebookKey("facebook_appname"), "missing_appname");
        return "http://apps.facebook.com/" + appname + "/";
    }

    /**
     * Constructs the Kontagent reporting URL for the supplied parameters.
     */
    public String getKontagentURL (Kontagent type, Object... keyVals)
    {
        long now = System.currentTimeMillis();

        // first construct the data we need to compute the signature
        List<String> args = Lists.newArrayList();
        for (int ii = 0; ii < keyVals.length; ii += 2) {
            if (keyVals[ii+1] != null) {
                keyVals[ii+1] = StringUtil.encode(String.valueOf(keyVals[ii+1]));
                args.add(keyVals[ii] + "=" + keyVals[ii+1]);
            }
        }
        args.add("ts=" + now);
        Collections.sort(args);
        args.add(_config.getValue("kontagent_secret", "secret"));

        // now construct the URL
        StringBuffer buf = new StringBuffer();
        buf.append("http://").append(_config.getValue("kontagent_server", "localhost"));
        buf.append("/api/v1/").append(_config.getValue("kontagent_key", "key")).append("/");
        buf.append(type.code).append("/?ts=").append(now);
        buf.append("&an_sig=").append(StringUtil.md5hex(Joiner.on("").join(args)));
        for (int ii = 0; ii < keyVals.length; ii += 2) {
            if (keyVals[ii+1] != null) {
                buf.append("&").append(keyVals[ii]).append("=").append(keyVals[ii+1]);
            }
        }

        return buf.toString();
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

    /**
     * Returns our embedded billing page URL.
     */
    public String getBillingURL ()
    {
        return _config.getValue("billing_url", (String)null);
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
        binds.add(new Binding.Servlet("/showinvite", ShowInviteServlet.class));
        binds.add(new Binding.Servlet("/upload", MediaUploadServlet.class));
        binds.add(new Binding.Servlet("/"+EverythingServlet.ENTRY_POINT, EverythingServlet.class));
        binds.add(new Binding.Servlet("/"+GameServlet.ENTRY_POINT, GameServlet.class));
        binds.add(new Binding.Servlet("/"+EditorServlet.ENTRY_POINT, EditorServlet.class));
        binds.add(new Binding.Servlet("/"+AdminServlet.ENTRY_POINT, AdminServlet.class));
        binds.add(Binding.Job.every(1, new Runnable() {
            public void run () {
                _gameLogic.processBirthdays();
            }
        }));
        binds.add(Binding.Job.every(1, new Runnable() {
            public void run () {
                int deleted = _playerRepo.pruneFeed(FEED_PRUNE_DAYS);
                if (deleted > 0) {
                    log.info("Pruned " + deleted + " old feed items.");
                }
            }
        }));
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
    public void coinsPurchased (int userId, int coins)
    {
        log.info("Player purchased coins, yay!", "user", userId, "coins", coins);
        _playerRepo.grantCoins(userId, coins);
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

    protected static final String KONTAGENT_API_URL = "http://api.geo.kontagent.net/api/v1/";
    protected static final int FEED_PRUNE_DAYS = 5;
}
