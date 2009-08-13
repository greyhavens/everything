//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntIntMap;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.Exps;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.FieldOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;

import com.threerings.everything.data.GridStatus;
import com.threerings.everything.data.News;
import com.threerings.everything.data.Powerup;

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

        // TODO: remove a week or two after 07-17-2009
        _ctx.registerMigration(PowerupRecord.class,
                               new SchemaMigration.Rename(2, "count", PowerupRecord.CHARGES));

        // TODO: remove a week or two after 08-03-2009
        _ctx.registerMigration(CardRecord.class,
                               new SchemaMigration.Rename(4, "created", CardRecord.RECEIVED));
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
     * Loads up the flipped status for the specified user's current grid.
     */
    public boolean[] loadFlipped (int userId)
    {
        FlippedRecord record = load(FlippedRecord.getKey(userId));
        return (record == null) ? null : record.toFlipped();
    }

    /**
     * Resets the flipped status for a user's grid to all unflipped. Used when creating a new grid
     * for the user.
     */
    public void resetFlipped (int userId)
    {
        FlippedRecord record = new FlippedRecord();
        record.userId = userId;
        store(record);
    }

    /**
     * Marks the specified position in the user's grid as flipped.
     *
     * @return true if the position was successfully transitioned from unflipped to flipped, false
     * if it was already flipped (or the user didn't exist).
     */
    public boolean flipPosition (int userId, int position)
    {
        return updatePartial(FlippedRecord.class,
                             new Where(Ops.and(FlippedRecord.USER_ID.eq(userId),
                                               FlippedRecord.SLOTS[position].eq(false))),
                             FlippedRecord.getKey(userId),
                             FlippedRecord.SLOTS[position], true) == 1;
    }

    /**
     * Resets the specified flip position for the specified user to unflipped.
     */
    public void resetPosition (int userId, int position)
    {
        updatePartial(FlippedRecord.getKey(userId), FlippedRecord.SLOTS[position], false);
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

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CardRecord.class);
        classes.add(FlippedRecord.class);
        classes.add(GridRecord.class);
        classes.add(NewsRecord.class);
        classes.add(PowerupRecord.class);
        classes.add(SeriesRecord.class);
        classes.add(EscrowedCardRecord.class);
    }
}
