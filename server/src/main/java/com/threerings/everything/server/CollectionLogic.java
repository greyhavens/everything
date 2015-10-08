//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.app.client.ServiceException;

import com.threerings.everything.data.Category;
import com.threerings.everything.data.PlayerCollection;
import com.threerings.everything.data.Series;
import com.threerings.everything.data.SeriesCard;
import com.threerings.everything.data.ThingCard;
import com.threerings.everything.rpc.GameService;
import com.threerings.everything.server.persist.CardRecord;
import com.threerings.everything.server.persist.GameRepository;
import com.threerings.everything.server.persist.LikeRecord;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;
import com.threerings.everything.server.persist.ThingRepository;

/**
 * Logic relating to a player's card collection.
 */
@Singleton
public class CollectionLogic {

    /**
     * Resolves the collection of the specified player.
     */
    public PlayerCollection getCollection (int ownerId) throws ServiceException
    {
        PlayerCollection coll = new PlayerCollection();
        coll.owner = _playerRepo.loadPlayerName(ownerId);
        if (coll.owner == null) {
            throw new ServiceException(GameService.E_UNKNOWN_USER);
        }

        // first load up all of the series
        List<SeriesCard> seriesCards = _thingRepo.loadPlayerSeries(ownerId);
        Multimap<Integer, SeriesCard> series = HashMultimap.create();
        // TODO: use loadPlayerThings and resolve category data from memory
        for (SeriesCard card : seriesCards) {
            series.put(card.parentId, card);
        }

        // load up the series' parents, the sub-categories
        Multimap<Integer, Category> subcats = HashMultimap.create();
        for (Category subcat : _thingRepo.loadCategories(series.keySet())) {
            subcats.put(subcat.parentId, subcat);
        }

        // finally load up the top-level categories and build everything back down
        coll.series = Maps.newTreeMap();
        for (Category cat : _thingRepo.loadCategories(subcats.keySet())) {
            Map<String, List<SeriesCard>> scats = Maps.newTreeMap();
            for (Category scat : subcats.get(cat.categoryId)) {
                List<SeriesCard> slist = Lists.newArrayList(series.get(scat.categoryId));
                Collections.sort(slist);
                scats.put(scat.name, slist);
            }
            coll.series.put(cat.name, scats);
        }

        // populate their trophies
        coll.trophies = _gameLogic.getTrophies(seriesCards);

        // populate their liked categories
        coll.likes = Sets.newHashSet();
        for (LikeRecord rec : _playerRepo.loadLikes(ownerId)) {
            if (rec.like) coll.likes.add(rec.categoryId);
        }

        return coll;
    }

    /**
     * Resolves data for the specified series.
     */
    public Series getSeries (int ownerId, int categoryId) throws ServiceException
    {
        Category category = _thingRepo.loadCategory(categoryId);
        if (category == null) {
            throw new ServiceException(GameService.E_UNKNOWN_SERIES);
        }

        // load up the cards owned by this player
        List<CardRecord> crecs = _gameRepo.loadCards(ownerId, categoryId);
        List<ThingCard> cards = Lists.newArrayList();

        // then load up all things in the series so that we can fill in blanks
        for (ThingCard thing : Sets.newTreeSet(_thingRepo.loadThingCards(categoryId))) {
            boolean added = false;
            for (CardRecord crec : crecs) {
                if (crec.thingId == thing.thingId) {
                    ThingCard card = ThingCard.clone(thing);
                    card.received = crec.received.getTime();
                    cards.add(card);
                    added = true;
                }
            }
            if (!added) {
                cards.add(null); // add a blank spot for this thing
            }
        }

        Series series = new Series();
        series.categoryId = categoryId;
        series.name = category.name;
        series.creator = _playerRepo.loadPlayerName(category.getCreatorId());
        series.things = cards.toArray(new ThingCard[cards.size()]);
        return series;
    }

    /**
     * Configures the specified player's like status for the specified category.
     */
    public void setLike (PlayerRecord player, int categoryId, Boolean like) throws ServiceException
    {
        // TODO: validate categoryId is valid (prevent malicious filling-up of the db?)
        _playerRepo.setLike(player.userId, categoryId, like);
    }

    @Inject protected GameLogic _gameLogic;
    @Inject protected ThingRepository _thingRepo;
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected GameRepository _gameRepo;
}
