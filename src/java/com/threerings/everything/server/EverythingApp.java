//
// $Id$

package com.threerings.everything.server;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileCleaner;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.samskivert.util.StringUtil;
import com.threerings.user.OOOUser;
import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.facebook.server.AbstractFacebookApp;
import com.threerings.samsara.app.server.AbstractSamsaraAppModule;
import com.threerings.samsara.shared.App;
import com.threerings.everything.client.Kontagent;
import com.threerings.everything.data.Build;
import com.threerings.everything.server.persist.PlayerRepository;
import static com.threerings.everything.Log.log;

/**
 * The main entry point for the Everything app.
 */
@Singleton
public class EverythingApp extends AbstractFacebookApp
{
    /** Our app identifier. */
    public static final String IDENT = "everything";

    public static class Module extends AbstractSamsaraAppModule
    {
        public Module () {
            super(IDENT);
        }

        @Override protected void configure () {
            super.configure();
            bind(App.class).to(EverythingApp.class);
            // bind our various servlets
            serve(AuthServlet.class).at("/auth");
            serve(InviteServlet.class).at("/invite");
            serve(ShowInviteServlet.class).at("/showinvite");
            serve(MediaUploadServlet.class).at("/upload");
            serve(CardImageServlet.class).at("/cardimg");
//            serve(TrophyImageServlet.class).at("/trophyimg");
            serve(EverythingServlet.class).at("/" +EverythingServlet.ENTRY_POINT);
            serve(GameServlet.class).at("/" + GameServlet.ENTRY_POINT);
            serve(EditorServlet.class).at("/" + EditorServlet.ENTRY_POINT);
            serve(AdminServlet.class).at("/" + AdminServlet.ENTRY_POINT);
            if (!_candidate) {
                schedule("process_birthdays", ProcessBirthdays.class).every(1);
                schedule("send_reminders", SendReminders.class).every(1);
                schedule("prune_records", PruneRecords.class).at(1);
            }
        }
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
        String url = getFacebookAppURL() + "?kc=" + type.code + "&t=" + tracking;
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
        String url = getFacebookAppURL() + "?vec=" + vector;
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
        args.add(_config.getValue("kontagent_secret", "secret"));

        // now construct the URL
        StringBuilder buf = new StringBuilder();
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
    public int getSiteId ()
    {
        return OOOUser.EVERYTHING_SITE_ID;
    }

    @Override // from App
    public void coinsPurchased (int userId, int coins)
    {
        log.info("Player purchased coins, yay!", "user", userId, "coins", coins);
        _playerRepo.grantCoins(userId, coins);
    }

    @Override // from App
    public void didAttach ()
    {
        log.info("Everything app initialized.", "version", _appvers, "build", Build.version());
    }

    @Override // from App
    public void didDetach ()
    {
        log.info("Everything app detached.", "version", _appvers);
        shutdownFileCleaner();
        // shut down our executors
        _executor.shutdown();
        // TODO: we want to wait for all of our pending servlets to finish before shutdown
        shutdown();
    }

    @SuppressWarnings("deprecation")
    protected void shutdownFileCleaner()
    {
        // Fileupload uses this internally to clean up its temp files, and its thread hangs around
        // forever eating permgen if we don't shut it down
        FileCleaner.exitWhenFinished();
    }

    protected ExecutorService _executor = Executors.newFixedThreadPool(3);

    @Inject protected @Named(AppCodes.APPVERS) String _appvers;
    @Inject protected @Named(AppCodes.APPCANDIDATE) boolean _candidate;
    @Inject protected PlayerRepository _playerRepo;

    protected static final String KONTAGENT_API_URL = "http://api.geo.kontagent.net/api/v1/";
    protected static final int FEED_PRUNE_DAYS = 5;
}
