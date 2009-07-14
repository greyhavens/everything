//
// $Id$

package com.threerings.everything.server;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;

import com.threerings.everything.data.PlayerName;
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
    public <T> List<T> resolveNames (List<T> objects, PlayerName... resolved)
    {
        // extract the set of ids we need to resolve
        ArrayIntSet ids = new ArrayIntSet();
        for (T object : objects) {
            try {
                for (Field field : getNameFields(object.getClass())) {
                    PlayerName name = (PlayerName)field.get(object);
                    if (name == null) {
                        log.warning("Skipping null name during resolution",
                                    "field", field.getName(), "object", object);
                    } else {
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
                    if (name != null) { // we already warned above on null names, so just skip
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

    @Inject protected PlayerRepository _playerRepo;
}
