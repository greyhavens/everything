//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntIntMap;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.Exps;
import com.samskivert.depot.Key;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.FieldOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;

import com.threerings.everything.data.CollectionStats;
import com.threerings.everything.data.GridStatus;
import com.threerings.everything.data.News;
import com.threerings.everything.data.Powerup;
import com.threerings.everything.data.SlotStatus;
import com.threerings.everything.server.ThingIndex;

import static com.threerings.everything.Log.log;

/**
 * Maintains game related persistent data.
 */
@Singleton
public class GameRepository extends DepotRepository
{
    @Inject public GameRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads up the latest news. The supplied array will contain zero or one elements.
     */
    public Collection<News> loadLatestNews ()
    {
        return findAll(NewsRecord.class,
                       OrderBy.descending(NewsRecord.REPORTED),
                       new Limit(0, 1)).map(NewsRecord.TO_NEWS);
    }

    /**
     * Stores a new news record into the database. Returns the time assigned to this news report.
     */
    public long reportNews (int reporterId, String text)
    {
        NewsRecord record = new NewsRecord();
        record.reported = new Timestamp(System.currentTimeMillis());
        record.reporterId = reporterId;
        record.text = text;
        insert(record);
        return record.reported.getTime();
    }

    /**
     * Updates the text of the specified news report.
     */
    public void updateNews (long reported, String text)
    {
        updatePartial(NewsRecord.getKey(new Timestamp(reported)),
                      NewsRecord.TEXT, text);
    }

    /**
     * Loads the specified card from the repository.
     */
    public CardRecord loadCard (int ownerId, int thingId, long received)
    {
        return load(CardRecord.getKey(ownerId, thingId, new Timestamp(received)));
    }

    /**
     * Loads all cards owned by the specified player in the specified category.
     */
    public List<CardRecord> loadCards (int ownerId, int categoryId)
    {
        return findAll(CardRecord.class,
                       CardRecord.THING_ID.join(ThingRecord.THING_ID),
                       new Where(Ops.and(CardRecord.OWNER_ID.eq(ownerId),
                                         ThingRecord.CATEGORY_ID.eq(categoryId))));
    }

    /**
     * Loads all cards owned by the specified player of the specified things.
     */
    public List<CardRecord> loadCards (int ownerId, Set<Integer> thingIds)
    {
        return findAll(CardRecord.class, new Where(Ops.and(CardRecord.OWNER_ID.eq(ownerId),
                                                           CardRecord.THING_ID.in(thingIds))));
    }

    /**
     * Loads all cards owned by the specified player.
     */
    public List<CardRecord> loadCards (int ownerId)
    {
        return findAll(CardRecord.class, new Where(CardRecord.OWNER_ID.eq(ownerId)));
    }

    /**
     * Loads and returns a mapping from category id to the id of all things held in that category
     * for the specified player.
     */
    public Multimap<Integer, Integer> loadCollection (int userId, ThingIndex index)
    {
        TreeMultimap<Integer, Integer> collection = TreeMultimap.create();
        for (Integer thingId : findAllKeys(CardRecord.class, false,
                                           new Where(CardRecord.OWNER_ID.eq(userId))).
                 map(Key.<CardRecord,Integer>extract(1))) {
            collection.put(index.getCategory(thingId), thingId);
        }
        return collection;
    }

    /**
     * Creates a new card for the specified player for the specified thing. Returns the newly
     * created card record.
     */
    public CardRecord createCard (int ownerId, int thingId, int giverId)
    {
        CardRecord card = new CardRecord();
        card.ownerId = ownerId;
        card.thingId = thingId;
        card.received = new Timestamp(System.currentTimeMillis());
        card.giverId = giverId;
        insert(card);
        return card;
    }

    /**
     * Deletes the specified card.
     */
    public void deleteCard (CardRecord card)
    {
        delete(card);
    }

    /**
     * Transfers the supplied card from it's current owner to the specified recipient.
     */
    public void giftCard (CardRecord card, int toUserId)
    {
        updatePartial(CardRecord.getKey(card.ownerId, card.thingId, card.received),
                      CardRecord.OWNER_ID, toUserId,
                      CardRecord.GIVER_ID, card.ownerId,
                      CardRecord.RECEIVED, new Timestamp(System.currentTimeMillis()));

        // update collection dirtiness and gift stats
        if (card.giverId == 0) {
            noteCardGifted(card.ownerId);
        } else {
            markCollectionDirty(card.ownerId);
        }
        markCollectionDirty(toUserId);
    }

    /**
     * Places the specified card into escrow, for delivery to the player that registers with the
     * supplied external id if and when they do so.
     */
    public void escrowCard (CardRecord card, String externalId)
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        // create an escrow record for this card
        EscrowedCardRecord erec = new EscrowedCardRecord();
        erec.externalId = externalId;
        erec.thingId = card.thingId;
        erec.created = now;
        erec.escrowed = now;
        insert(erec);

        // gift the card to player -1 to remove it from the gifter's collection (we have to do this
        // manually because we need to be sure that the timestamp is *exactly* the same)
        updatePartial(CardRecord.getKey(card.ownerId, card.thingId, card.received),
                      CardRecord.OWNER_ID, -1,
                      CardRecord.GIVER_ID, card.ownerId,
                      CardRecord.RECEIVED, now);

        // note that their collection status changed
        markCollectionDirty(card.ownerId);
    }

    /**
     * Transfers any cards from escrowd into the collection of the newly created player.
     */
    public void unescrowCards (String externalId, PlayerRecord player)
    {
        Where where = new Where(EscrowedCardRecord.EXTERNAL_ID.eq(externalId));
        for (EscrowedCardRecord card : findAll(EscrowedCardRecord.class, where)) {
            // transfer the card to the player
            log.info("Transfering escrowed card to new player", "card", card.thingId,
                     "player", player.who());
            if (updatePartial(CardRecord.getKey(-1, card.thingId, card.created),
                              CardRecord.OWNER_ID, player.userId) == 0) {
                log.warning("Failed to transfer escrowed card?", "card", card.thingId,
                            "created", card.created, "to", player.who());
            }
            // delete the escrow record
            delete(card);
        }
    }

    /**
     * Counts up and returns the number of cards in the supplied set of things that are held by
     * each of the specified owners.
     */
    public IntIntMap countCardHoldings (Set<Integer> ownerIds, Set<Integer> thingIds)
    {
        IntIntMap data = new IntIntMap();
        for (OwnerRecord orec : findAll(OwnerRecord.class,
                                        new FieldOverride(OwnerRecord.COUNT,
                                                          Exps.countDistinct(CardRecord.THING_ID)),
                                        new Where(Ops.and(CardRecord.OWNER_ID.in(ownerIds),
                                                          CardRecord.THING_ID.in(thingIds))),
                                        new GroupBy(CardRecord.OWNER_ID))) {
            data.put(orec.ownerId, orec.count);
        }
        return data;
    }

    /**
     * Notes that the player in question has completed the specified series.
     *
     * @return true if this should be reported to the feed, false if they had already completed the
     * series and must have just sold off a card and obtained it again.
     */
    public boolean noteCompletedSeries (int userId, int categoryId)
    {
        try {
            SeriesRecord record = new SeriesRecord();
            record.userId = userId;
            record.categoryId = categoryId;
            record.when = new Timestamp(System.currentTimeMillis());
            insert(record);
            return true;
        } catch (DuplicateKeyException dke) {
            return false;
        }
    }

    /**
     * Loads the specified player's current grid.
     */
    public GridRecord loadGrid (int userId)
    {
        return load(GridRecord.getKey(userId));
    }

    /**
     * Stores the supplied grid in the repository, overwriting any previous grid for the owning
     * player.
     */
    public void storeGrid (GridRecord record)
    {
        store(record);
    }

    /**
     * Updates the status of the supplied grid.
     */
    public void updateGridStatus (GridRecord record, GridStatus status)
    {
        updatePartial(GridRecord.getKey(record.userId), GridRecord.STATUS, status);
        record.status = status; // update the in-memory copy
    }

    /**
     * Loads up the slot status for the specified user's current grid.
     */
    public SlotStatusRecord loadSlotStatus (int userId)
    {
        return load(SlotStatusRecord.getKey(userId));
    }

    /**
     * Resets the slot status for a user's grid. Used when creating a new grid for the user.
     */
    public void resetSlotStatus (int userId)
    {
        SlotStatusRecord record = new SlotStatusRecord();
        record.userId = userId;
        record.status0 = SlotStatus.UNFLIPPED;
        record.status1 = SlotStatus.UNFLIPPED;
        record.status2 = SlotStatus.UNFLIPPED;
        record.status3 = SlotStatus.UNFLIPPED;
        record.status4 = SlotStatus.UNFLIPPED;
        record.status5 = SlotStatus.UNFLIPPED;
        record.status6 = SlotStatus.UNFLIPPED;
        record.status7 = SlotStatus.UNFLIPPED;
        record.status8 = SlotStatus.UNFLIPPED;
        record.status9 = SlotStatus.UNFLIPPED;
        record.status10 = SlotStatus.UNFLIPPED;
        record.status11 = SlotStatus.UNFLIPPED;
        record.status12 = SlotStatus.UNFLIPPED;
        record.status13 = SlotStatus.UNFLIPPED;
        record.status14 = SlotStatus.UNFLIPPED;
        record.status15 = SlotStatus.UNFLIPPED;
        store(record);
    }

    /**
     * Marks the specified position in the user's grid as flipped.
     *
     * @return true if the position was successfully transitioned from unflipped to flipped, false
     * if it was already flipped (or the user didn't exist).
     */
    public boolean flipSlot (int userId, int position)
    {
        return updatePartial(SlotStatusRecord.class,
                             new Where(Ops.and(SlotStatusRecord.USER_ID.eq(userId),
                                               SlotStatusRecord.STATUSES[position].eq(
                                                   SlotStatus.UNFLIPPED))),
                             SlotStatusRecord.getKey(userId),
                             SlotStatusRecord.STATUSES[position], SlotStatus.FLIPPED) == 1;
    }

    /**
     * Resets the specified flip position for the specified user to unflipped.
     */
    public void resetSlot (int userId, int position)
    {
        updatePartial(SlotStatusRecord.getKey(userId),
                      SlotStatusRecord.STATUSES[position], SlotStatus.UNFLIPPED,
                      SlotStatusRecord.STAMPS[position], 0L);
    }

    /**
     * Updates the flipped stamp for the specified slot.
     */
    public void updateSlot (int userId, int position, long stamp)
    {
        updatePartial(SlotStatusRecord.getKey(userId),
                      SlotStatusRecord.STAMPS[position], stamp);
    }

    /**
     * Updates the status for the specified slot. Requires that the old status match the specified
     * "from" status otherwise no change will be made.
     */
    public void updateSlot (int userId, int position, SlotStatus from, SlotStatus to)
    {
        updatePartial(SlotStatusRecord.class,
                      new Where(Ops.and(SlotStatusRecord.USER_ID.eq(userId),
                                        SlotStatusRecord.STATUSES[position].eq(from))),
                      SlotStatusRecord.getKey(userId),
                      SlotStatusRecord.STATUSES[position], to);
    }

    /**
     * Loads and returns the specified player's powerup inventory.
     */
    public Map<Powerup, Integer> loadPowerups (int ownerId)
    {
        Map<Powerup, Integer> inventory = Maps.newHashMap();
        for (PowerupRecord record : findAll(PowerupRecord.class,
                                            new Where(PowerupRecord.OWNER_ID.eq(ownerId)))) {
            inventory.put(record.type, record.charges);
        }
        return inventory;
    }

    /**
     * Returns the number of powerup charges possessed by the specified player for the specified
     * powerup type.
     */
    public int loadPowerupCount (int ownerId, Powerup type)
    {
        PowerupRecord record = load(PowerupRecord.getKey(ownerId, type));
        return (record == null) ? 0 : record.charges;
    }

    /**
     * Grants powerup charges for the specified type to the specified player.
     */
    public void grantPowerupCharges (int ownerId, Powerup type, int charges)
    {
        // first try updating an existing record
        if (updatePartial(PowerupRecord.getKey(ownerId, type),
                          PowerupRecord.CHARGES, PowerupRecord.CHARGES.plus(charges)) > 0) {
            return;
        }

        // if that fails, insert a new record
        PowerupRecord record = new PowerupRecord();
        record.ownerId = ownerId;
        record.type = type;
        record.charges = charges;
        insert(record);

        // note: the above insertion could fail if the player was somehow making a purchase from
        // two computers at the exact same time, but why would they be doing that?
    }

    /**
     * Consumes one charge of the specified player's powerup.
     *
     * @return true if the charge was consumed, false if the player did not have sufficient charges
     * (or did not exist).
     */
    public boolean consumePowerupCharge (int userId, Powerup type)
    {
        return updatePartial(PowerupRecord.class,
                             new Where(Ops.and(PowerupRecord.OWNER_ID.eq(userId),
                                               PowerupRecord.TYPE.eq(type),
                                               PowerupRecord.CHARGES.greaterEq(1))),
                             PowerupRecord.getKey(userId, type),
                             PowerupRecord.CHARGES, PowerupRecord.CHARGES.minus(1)) == 1;
    }

    /**
     * Notes that the collection of the specified user needs to be updated.
     */
    public void markCollectionDirty (int userId)
    {
        int mods = updatePartial(CollectionRecord.class,
                                 new Where(Ops.and(CollectionRecord.USER_ID.eq(userId),
                                                   CollectionRecord.NEEDS_UPDATE.eq(true))),
                                 CollectionRecord.getKey(userId),
                                 CollectionRecord.NEEDS_UPDATE, true);
        if (mods == 0) {
            CollectionRecord record = new CollectionRecord();
            record.userId = userId;
            record.needsUpdate = true;
            insert(record);
        }
    }

    /**
     * Notes that the user in question gifted a flipped card. Also marks their collection as
     * needing resummarizing.
     */
    public void noteCardGifted (int userId)
    {
        updatePartial(CollectionRecord.getKey(userId),
                      CollectionRecord.GIFTS, CollectionRecord.GIFTS.plus(1),
                      CollectionRecord.NEEDS_UPDATE, true);
    }

    /**
     * Returns stats on the collections of the specified set of players. This will cause the
     * collection summaries to be updated for any player that needs it. The names in the resulting
     * records will need to be resolved by the caller.
     */
    public List<CollectionStats> loadCollectionStats (Set<Integer> owners, ThingIndex index)
    {
        Map<Integer, CollectionRecord> stats = Maps.newHashMap();
        Set<Integer> updates = Sets.newHashSet();
        for (CollectionRecord record : findAll(CollectionRecord.class,
                                               new Where(CollectionRecord.USER_ID.in(owners)))) {
            if (record.needsUpdate) {
                updates.add(record.userId);
            }
            stats.put(record.userId, record);
        }
        for (Integer updaterId : updates) {
            stats.put(updaterId, updateCollectionStats(updaterId, index));
        }
        return Lists.newArrayList(Iterables.transform(stats.values(), CollectionRecord.TO_STATS));
    }

    /**
     * Updates the collection summary stats for the specified user.
     */
    protected CollectionRecord updateCollectionStats (int userId, ThingIndex index)
    {
        // compute their updated data
        CollectionRecord record = new CollectionRecord();
        Multimap<Integer, Integer> coll = loadCollection(userId, index);
        for (Map.Entry<Integer, Collection<Integer>> entry : coll.asMap().entrySet()) {
            record.things += entry.getValue().size();
            record.series++;
            if (index.getCategorySize(entry.getKey()) == entry.getValue().size()) {
                record.completeSeries++;
            }
        }

        int mods = updatePartial(CollectionRecord.getKey(userId),
                                 CollectionRecord.THINGS, record.things,
                                 CollectionRecord.SERIES, record.series,
                                 CollectionRecord.COMPLETE_SERIES, record.completeSeries,
                                 CollectionRecord.NEEDS_UPDATE, false);
        if (mods == 0) {
            record.userId = userId;
            store(record);
        }
        return load(CollectionRecord.getKey(userId));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CardRecord.class);
        classes.add(CollectionRecord.class);
        classes.add(EscrowedCardRecord.class);
        classes.add(GridRecord.class);
        classes.add(NewsRecord.class);
        classes.add(PowerupRecord.class);
        classes.add(SeriesRecord.class);
        classes.add(SlotStatusRecord.class);
    }
}
