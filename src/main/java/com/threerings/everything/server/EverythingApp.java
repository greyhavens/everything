//
// $Id$

package com.threerings.everything.server;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.Config;
import com.samskivert.util.Lifecycle;
import com.samskivert.util.StringUtil;

import com.threerings.app.server.AppHttpServer;
import com.threerings.facebook.servlet.FacebookConfig;
import com.threerings.user.OOOUser;
import com.threerings.util.PostgresUtil;

import com.threerings.everything.data.Build;
import com.threerings.everything.rpc.Kontagent;
import com.threerings.everything.server.credits.CreditsServlet;
import com.threerings.everything.server.persist.PlayerRepository;

import static com.threerings.everything.Log.log;

/**
 * The main entry point for the Everything app.
 */
@Singleton
public class EverythingApp
{
    /** Our app identifier. */
    public static final String IDENT = "everything";

    /** Used to identify the {@link File} that contains our app root. */
    public static final String APPROOT = "approot";

    public static class Module extends AbstractModule {
        @Override protected void configure () {
            bind(PersistenceContext.class).toInstance(new PersistenceContext());
            bind(Lifecycle.class).toInstance(new Lifecycle());
            bind(FacebookConfig.class).toInstance(new FacebookConfig() {
                @Override public String getFacebookKey () {
                    return getenv("FACEBOOK_KEY", null);
                }
                @Override public String getFacebookSecret () {
                    return getenv("FACEBOOK_SECRET", null);
                }
                @Override public String getFacebookAppId () {
                    return getenv("FACEBOOK_APPID", null);
                }
                @Override public String getFacebookAppName () {
                    return getenv("FACEBOOK_APPNAME", null);
                }
            });
            File approot = new File(System.getProperty("approot"));
            bind(File.class).annotatedWith(Names.named(APPROOT)).toInstance(approot);
        }
    }

    public static void main (String[] args) {
        Injector injector = Guice.createInjector(new Module());
        long start = System.currentTimeMillis();

        // create our database connection
        PersistenceContext perCtx = injector.getInstance(PersistenceContext.class);
        try {
            String dbenv = getenv("DATABASE_URL", null);
            if (dbenv == null) {
                throw new RuntimeException("Must supply 'DATABASE_URL' environment variable.");
            }
            // swap postgres: for http: otherwise URL freaks out
            URL dburl = new URL(dbenv.replaceAll("postgres:", "http:"));
            // TODO: validate (regexp?) that it has the form: postgres://username:password@host/dbname

            Properties dbprops = new Properties();
            dbprops.setProperty("db.default.server", dburl.getHost());
            int port = dburl.getPort();
            dbprops.setProperty("db.default.port", String.valueOf(port == -1 ? POSTGRES_PORT : port));
            dbprops.setProperty("db.default.database", dburl.getPath().substring(1));
            String[] uinfo = dburl.getUserInfo().split(":");
            dbprops.setProperty("db.default.username", uinfo[0]);
            dbprops.setProperty("db.default.password", uinfo[1]);
            // TODO: maxconns?

            String dbid = IDENT + start;
            ConnectionProvider conprov = PostgresUtil.createPoolingProvider(
                new Config("db", dbprops), dbid);
            // Initialize our app persistence context
            perCtx.init(IDENT, conprov, null);

        } catch (Throwable t) {
            log.error("Database initialization failed", t);
            System.exit(255);
        }

        // create and initialize the whole shebang
        EverythingApp app = injector.getInstance(EverythingApp.class);
        app.init();

        // initialize our database repositories and run migrations (now that all the servlets are
        // injected, they will have been created and registered)
        perCtx.initializeRepositories(true);

        // initialize everything that is registered with Lifecycle
        Lifecycle cycle = injector.getInstance(Lifecycle.class);
        cycle.init();

        // when run() returns, a shutdown will have been requested
        app.run();
        app.shutdown();
    }

    /**
     * Returns an executor that can be used for background processing tasks.
     */
    public Executor getExecutor ()
    {
        return _executor;
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
        String url = _fbconf.getFacebookAppURL("http") + "?kc=" + type.code + "&t=" + tracking;
        if (args.length > 0) {
            url += "&token=" + Joiner.on("~").join(args);
        }
        return url;
    }

    /**
     * Returns a URL to our Facebook app that we can put out in the wide world.
     *
     * @param vector an identifier used to track this URL, e.g. "reminder_2".
     */
    public String getHelloURL (String vector, Object... args)
    {
        String url = _fbconf.getFacebookAppURL("http") + "?vec=" + vector;
        if (args.length > 0) {
            url += "&token=" + Joiner.on("~").join(args);
        }
        return url;
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
        args.add(getenv("KONTAGENT_SECRET", "secret"));

        // now construct the URL
        StringBuilder buf = new StringBuilder();
        buf.append("http://").append(getenv("KONTAGENT_SERVER", "localhost"));
        buf.append("/api/v1/").append(getenv("KONTAGENT_KEY", "key")).append("/");
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
    public String getMediaStoreId () {
        return getenv("MEDIASTORE_ID", null);
    }

    /**
     * Returns the secret key to our S3 media store.
     */
    public String getMediaStoreKey () {
        return getenv("MEDIASTORE_KEY", null);
    }

    /**
     * Returns the S3 bucket to which to upload when saving to our media store.
     */
    public String getMediaStoreBucket () {
        return getenv("MEDIASTORE_BUCKET", null);
    }

    /**
     * Returns our embedded billing page URL.
     */
    public String getBillingURL () {
        return getenv("BILLING_URL", null);
    }

    /**
     * Returns the url for this app as hosted by samsara.
     */
    public String getBaseUrl () {
        return _config.getValue("samsara_base_url", getHostUrl());
    }

    public void coinsPurchased (int userId, int coins) {
        log.info("Player purchased coins, yay!", "user", userId, "coins", coins);
        _playerRepo.grantCoins(userId, coins);
    }

    public void init () {
        // wire up all of our servlets
        _https.init(getHttpPort(), _approot);
        _https.serve(AuthServlet.class, "/auth");
        _https.serve(InviteServlet.class, "/invite");
        _https.serve(ShowInviteServlet.class, "/showinvite");
        _https.serve(MediaUploadServlet.class, "/upload");
        _https.serve(CardImageServlet.class, "/cardimg");
        _https.serve(CreditsServlet.class, "/fbcredits");
        String gwtRoot = "/everything/";
        _https.serve(EverythingServlet.class, gwtRoot + EverythingServlet.ENTRY_POINT);
        _https.serve(GameServlet.class, gwtRoot + GameServlet.ENTRY_POINT);
        _https.serve(EditorServlet.class, gwtRoot + EditorServlet.ENTRY_POINT);
        _https.serve(AdminServlet.class, gwtRoot + AdminServlet.ENTRY_POINT);

        // set up our cron jobs
        // schedule("process_birthdays", ProcessBirthdays.class).every(1);
        // schedule("send_reminders", SendReminders.class).every(1);
        // schedule("prune_records", PruneRecords.class).at(1);

        log.info("Everything app initialized.", "build", Build.version());
    }

    public void run () {
        // start the http server, let the app run run run, and then we're done done done
        try {
            _https.start();
            // wait for all of our pending servlets to finish before we exit
            _https.join();
        } catch (Throwable t) {
            log.error("HTTP server failure", t);
        }
    }

    public void shutdown () {
        // shut down the http server
        _https.shutdown();
        // shut down our executors
        _executor.shutdown();
        // shutdown our persistence context
        _perCtx.shutdown();
        log.info("Everything app shutdown.");
    }

    protected String getHostUrl () {
        int port = getHttpPort();
        return "http://localhost" + (port == 80 ? "" : (":" + port));
    }

    protected int getHttpPort () {
        return Integer.parseInt(getenv("PORT", "8080"));
    }

    protected static String getenv (String name, String defval) {
        String value = System.getenv(name);
        return (value == null) ? defval : value;
    }

    protected static class ProcessBirthdays implements Runnable {
        @Override public void run () {
            _gameLogic.processBirthdays();
        }
        @Inject GameLogic _gameLogic;
    }

    protected static class SendReminders implements Runnable {
        @Override public void run () {
            _playerLogic.sendReminderNotifications();
        }
        @Inject PlayerLogic _playerLogic;
    }

    protected static class PruneRecords implements Runnable {
        @Override public void run () {
            int feed = _playerRepo.pruneFeed(FEED_PRUNE_DAYS);
            int recruit = _playerRepo.pruneGiftRecords();
            if (feed > 0) {
                log.info("Pruned " + feed + " old feed items.");
            }
            if (recruit > 0) {
                log.info("Pruned " + recruit + " old recruitment gift records.");
            }
            // TODO: prune GridRecords, but only after the maximum number of free flips
            // can be accumulated into the PlayerRecord
        }
        @Inject PlayerRepository _playerRepo;
    }

    protected ExecutorService _executor = Executors.newFixedThreadPool(3);

    protected final Config _config = new Config("everything"); // TODO

    @Inject protected @Named(APPROOT) File _approot;
    @Inject protected PersistenceContext _perCtx;
    @Inject protected AppHttpServer _https;
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected FacebookConfig _fbconf;

    protected static final int POSTGRES_PORT = 5432;
    protected static final String KONTAGENT_API_URL = "http://api.geo.kontagent.net/api/v1/";
    protected static final int FEED_PRUNE_DAYS = 5;
}
