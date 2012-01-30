//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Calendars;
import com.samskivert.util.Tuple;

import com.threerings.app.server.UserLogic;
import com.threerings.user.ExternalAuther;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.rpc.GameCodes;
import com.threerings.everything.rpc.Kontagent;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;

import static com.threerings.everything.Log.log;

/**
 * Provides player related services.
 */
@Singleton
public class PlayerLogic
{
    /**
     * Resolves all names in the supplied list of objects.
     */
    public <T, L extends Iterable<T>> L resolveNames (L objects, PlayerName... resolved)
    {
        // extract the set of ids we need to resolve
        Set<Integer> ids = Sets.newHashSet();
        for (T object : objects) {
            try {
                for (Field field : getNameFields(object.getClass())) {
                    PlayerName name = (PlayerName)field.get(object);
                    if (name != null) {
                        ids.add(name.userId);
                    }
                }
            } catch (Exception e) {
                log.warning("Failed to extract name fields", "object", object, e);
            }
        }

        // remove the ids of the names we already have
        for (PlayerName name : resolved) {
            ids.remove(name.userId);
        }

        // load up the to resolve names
        Map<Integer, PlayerName> names = _playerRepo.loadPlayerNames(ids);

        // add in the ones we already have
        for (PlayerName name : resolved) {
            names.put(name.userId, name);
        }

        // and finally write the resolved names back to the objects
        for (T object : objects) {
            try {
                for (Field field : getNameFields(object.getClass())) {
                    PlayerName name = (PlayerName)field.get(object);
                    if (name != null) {
                        field.set(object, names.get(name.userId));
                    }
                }
            } catch (Exception e) {
                log.warning("Failed to write name fields", "object", object, e);
            }
        }

        // return the list of objects for convenient chaining
        return objects;
    }

    /**
     * Sends a card gifting notification to the specified Facebook player.
     */
    public void sendGiftNotification (PlayerRecord sender, long toFBId, Category series)
    {
        // String tracking = _kontLogic.generateUniqueId(sender.userId);
        // String url = _app.getHelloURL(Kontagent.NOTIFICATION, tracking);

        // BundleActionLink link = new BundleActionLink();
        // link.setText("Open gift");
        // link.setHref(url);
        // DashboardNewsItem item = new DashboardNewsItem();
        // item.setMessage(sender.name + " gave you a card in the " + series.name + " series.");
        // item.setActionLink(link);

        // sendFacebookNotification(sender, toFBId, item, tracking);
    }

    // /**
    //  * Delivers a notification to the specified Facebook user from the specified sender.
    //  */
    // public void sendFacebookNotification (
    //     PlayerRecord from, final long toFBId, final DashboardNewsItem item, final String tracking)
    // {
    //     if (toFBId == 0) {
    //         return; // noop, some day we'll support players who aren't on Facebook
    //     }

    //     final Tuple<String, String> finfo = _userLogic.getExtAuthInfo(
    //         ExternalAuther.FACEBOOK, from.userId);
    //     if (finfo == null || finfo.right == null) {
    //         log.warning("Missing Facebook data for notification", "from", from.who(),
    //                     "finfo", finfo);
    //         return;
    //     }

    //     final FacebookJaxbRestClient fbclient = _faceLogic.getFacebookClient(finfo.right);
    //     _app.getExecutor().execute(new Runnable() {
    //         public void run () {
    //             try {
    //                 log.debug("Sending FB notification", "id", toFBId, "item", item);
    //                 // send the notification to Facebook
    //                 fbclient.dashboard_multiAddNews(
    //                     Collections.singleton(toFBId), Collections.singleton(item));

    //                 // disabled Kontagent for now since we don't really care
    //                 //
    //                 // tell Kontagent that we sent a notification
    //                 // _kontLogic.reportAction(
    //                 //     Kontagent.NOTIFICATION, "s", finfo.left, "r", toFBId, "u", tracking);
    //             } catch (Exception e) {
    //                 log.info("Failed to send Facebook notification", "to", toFBId,
    //                          "item", item, "error", e.getMessage());
    //             }
    //         }
    //     });
    // }

    /**
     * Sends notifications to all players whose last session fell during the current hour exactly
     * two, four or six days ago. This is called from an hourly cronjob.
     */
    public void sendReminderNotifications ()
    {
        // final Set<Long> two = Sets.newHashSet(), four = Sets.newHashSet(), six = Sets.newHashSet();
        // Calendars.Builder cal = Calendars.now().set(Calendar.MINUTE, 0).set(Calendar.SECOND, 0);
        // long threeDays = cal.addDays(-3).toTime(), fiveDays = cal.addDays(-2).toTime();
        // for (PlayerRecord prec : _playerRepo.loadIdlePlayers()) {
        //     if (prec.lastSession.getTime() < fiveDays) {
        //         six.add(prec.facebookId);
        //     } else if (prec.lastSession.getTime() < threeDays) {
        //         four.add(prec.facebookId);
        //     } else {
        //         two.add(prec.facebookId);
        //     }
        // }
        // sendReminderNotifications(two, 2);
        // sendReminderNotifications(four, 4);
        // sendReminderNotifications(six, 6);
        // if (two.size() > 0 || four.size() > 0 || six.size() > 0) {
        //     log.info("Send Facebook reminder notifications", "two", two.size(), "four", four.size(),
        //              "six", six.size());
        // }
    }

    /**
     * Sends reminder notifications to the specified set of Facebook ids who have been idle for the
     * specified number of days. This is only broken out for testing.
     */
    public void sendReminderNotifications (final Set<Long> fbids, int idleDays)
    {
        // if (fbids.isEmpty()) {
        //     return; // noop!
        // }

        // int flips = GameCodes.DAILY_FREE_FLIPS + idleDays - 1; // reasonable estimate

        // BundleActionLink link = new BundleActionLink();
        // link.setText("Flip cards");
        // link.setHref(_app.getHelloURL("reminder" + idleDays));
        // final DashboardNewsItem item = new DashboardNewsItem();
        // item.setMessage("You have " + flips + " free card flips waiting for you.");
        // item.setActionLink(link);

        // final FacebookJaxbRestClient fbclient = _faceLogic.getFacebookClient();
        // _app.getExecutor().execute(new Runnable() {
        //     public void run () {
        //         for (List<Long> ids : Iterables.partition(fbids, 50)) {
        //             try {
        //                 fbclient.dashboard_multiAddNews(ids, Collections.singleton(item));
        //             } catch (Exception e) {
        //                 log.info("Failed to send Facebook reminder notification", "item", item,
        //                          "error", e.getMessage());
        //             }
        //         }
        //     }
        // });
    }

    protected List<Field> getNameFields (Class<?> clazz)
    {
        List<Field> fields = _nameFields.get(clazz);
        if (fields != null) {
            return fields;
        }

        fields = Lists.newArrayList();
        for (Field field : clazz.getFields()) {
            if (field.getType().equals(PlayerName.class)) {
                fields.add(field);
            }
        }
        _nameFields.put(clazz, fields);
        return fields;
    }

    protected Map<Class<?>, List<Field>> _nameFields =
        new ConcurrentHashMap<Class<?>, List<Field>>();

    @Inject protected EverythingApp _app;
    @Inject protected KontagentLogic _kontLogic;
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected UserLogic _userLogic;
}
