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

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.Tuple;

import com.threerings.everything.data.PlayerName;
import com.threerings.samsara.app.server.UserLogic;
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
     * Delivers a notification to the specified Facebook user from the specified sender.
     */
    public void sendFacebookNotification (PlayerRecord from, PlayerRecord to, final String fbml)
    {
        final Tuple<String, String> finfo = _userLogic.getFacebookAuthInfo(from.userId);
        final Tuple<String, String> tinfo = _userLogic.getFacebookAuthInfo(to.userId);
        if (finfo == null || finfo.right == null || tinfo == null) {
            log.warning("Missing needed Facebook data for notification", "from", from.who(),
                        "to", to.who(), "finfo", finfo, "tinfo", tinfo);
            return;
        }

        final FacebookJaxbRestClient fbclient = _faceLogic.getFacebookClient(finfo.right);
        _app.getExecutor().execute(new Runnable() {
            public void run () {
                try {
                    fbclient.notifications_send(
                        Collections.singleton(Long.parseLong(tinfo.left)), fbml);
                } catch (Exception e) {
                    log.info("Failed to send Facebook notification", "to", tinfo.left,
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
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected UserLogic _userLogic;
}
