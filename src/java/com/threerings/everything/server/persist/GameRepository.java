//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;

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
     * Creates a new card for the specified player for the specified thing. Returns the newly
     * created card record.
     */
    public CardRecord createCard (int ownerId, int thingId)
    {
        CardRecord card = new CardRecord();
        card.ownerId = ownerId;
        card.thingId = thingId;
        card.created = new Timestamp(System.currentTimeMillis());
        insert(card);
        return card;
    }

    /**
     * Loads the specified card from the repository.
     */
    public CardRecord loadCard (int ownerId, int thingId, long created)
    {
        return load(CardRecord.getKey(ownerId, thingId, new Timestamp(created)));
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

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CardRecord.class);
        classes.add(FlippedRecord.class);
        classes.add(GridRecord.class);
        classes.add(PowerupRecord.class);
    }
}
