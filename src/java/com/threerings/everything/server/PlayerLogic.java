//
// $Id$

package com.threerings.everything.server;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.google.code.facebookapi.FacebookJaxbRestClient;

import com.samskivert.servlet.util.HTMLUtil;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.samsara.app.server.UserLogic;

import com.threerings.everything.client.Kontagent;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.PlayerName;
import com.threerings.everything.data.Thing;
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
        ArrayIntSet ids = new ArrayIntSet();
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
        IntMap<PlayerName> names = _playerRepo.loadPlayerNames(ids);

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

        // return the list of objects for convenient chainig
        return objects;
    }

    /**
     * Posts a story to the specified player's Facebook feed.
     */
    public void postFacebookStory (PlayerRecord user, String todo)
    {
        Tuple<String, String> fbinfo = _userLogic.getFacebookAuthInfo(user.userId);
        if (fbinfo == null || fbinfo.right == null) {
            log.warning("Can't post Facebook story, have no Facebook authinfo?", "who", user.who(),
                        "fbinfo", fbinfo);
            return;
        }
        // TODO
    }

    /**
     * Sends a card gifting notification to the specified Facebook player.
     */
    public void sendGiftNotification (PlayerRecord sender, long toFBId, Thing thing,
                                      Category series, boolean completed, String message)
    {
        String tracking = _kontLogic.generateUniqueId(sender.userId);
        String browseURL = _app.getHelloURL(
            Kontagent.NOTIFICATION, tracking, "BROWSE", "", thing.categoryId);
        String everyURL = _app.getHelloURL(Kontagent.NOTIFICATION, tracking);

        // TODO: localization?
        String feedmsg;
        if (completed) {
            feedmsg = String.format(
                "gave you the <a href=\"%s\">%s</a> card in <a href=\"%s\">Everything</a> and " +
                "completed your <a href=\"%s\">%s</a> series!",
                browseURL, thing.name, everyURL, browseURL, series.name);
        } else {
            feedmsg = String.format( // TODO: localization?
                "gave you the <a href=\"%s\">%s</a> card in <a href=\"%s\">Everything</a>.",
                browseURL, thing.name, everyURL);
        }

        if (!StringUtil.isBlank(message)) {
            feedmsg += " They said '" + HTMLUtil.entify(message) + "'.";
        }
        sendFacebookNotification(sender, toFBId, feedmsg, tracking);
    }

    /**
     * Delivers a notification to the specified Facebook user from the specified sender.
     */
    public void sendFacebookNotification (PlayerRecord from, final long toFBId,
                                          final String fbml, final String tracking)
    {
        final Tuple<String, String> finfo = _userLogic.getFacebookAuthInfo(from.userId);
        if (finfo == null || finfo.right == null) {
            log.warning("Missing Facebook data for notification", "from", from.who(),
                        "finfo", finfo);
            return;
        }

        final FacebookJaxbRestClient fbclient = _faceLogic.getFacebookClient(finfo.right);
        _app.getExecutor().execute(new Runnable() {
            public void run () {
                try {
                    // send the notification to Facebook
                    fbclient.notifications_send(Collections.singleton(toFBId), fbml);
                    // tell Kontagent that we sent a notification
                    _kontLogic.reportAction(
                        Kontagent.NOTIFICATION, "s", finfo.left, "r", toFBId, "u", tracking);
                } catch (Exception e) {
                    log.info("Failed to send Facebook notification", "to", toFBId,
                             "fbml", fbml, "error", e.getMessage());
                }
            }
        });
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
    @Inject protected FacebookLogic _faceLogic;
    @Inject protected KontagentLogic _kontLogic;
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected UserLogic _userLogic;
}
