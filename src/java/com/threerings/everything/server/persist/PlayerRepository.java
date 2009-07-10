//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;

import com.threerings.everything.client.GameCodes;
import com.threerings.everything.data.GameStatus;
import com.threerings.everything.data.PlayerName;

/**
 * Manages player state for the Everything app.
 */
@Singleton
public class PlayerRepository extends DepotRepository
{
    @Inject public PlayerRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads and returns the player with the specified user id, or null if they don't exist.
     */
    public PlayerRecord loadPlayer (int userId)
    {
        return load(PlayerRecord.getKey(userId));
    }

    /**
     * Load and returns the name of the specified player, or null if they don't exist.
     */
    public PlayerName loadPlayerName (int userId)
    {
        return PlayerRecord.TO_NAME.apply(loadPlayer(userId));
    }

    /**
     * Creates, inserts and returns a new player record.
     */
    public PlayerRecord createPlayer (int userId, String name, long birthday)
    {
        PlayerRecord record = new PlayerRecord();
        record.userId = userId;
        record.name = name;
        record.birthday = (birthday == 0L) ? null : new Date(birthday);
        record.joined = new Timestamp(System.currentTimeMillis());
        record.lastSession = record.joined;
        record.coins = GameCodes.NEW_USER_FREE_COINS;
        record.freeFlips = GameCodes.NEW_USER_FREE_FLIPS;
        insert(record);
        return record;
    }

    /**
     * Updates the specified user's last session stamp and grants them free flips earned since
     * their previous session.
     */
    public void recordSession (int userId, long sessionStamp, float freeFlipsEarned)
    {
        updatePartial(PlayerRecord.getKey(userId),
                      PlayerRecord.LAST_SESSION, new Timestamp(sessionStamp),
                      PlayerRecord.FREE_FLIPS, PlayerRecord.FREE_FLIPS.plus(freeFlipsEarned));
    }

    /**
     * Grants the specified number of coins to the specified player.
     */
    public void grantCoins (int userId, int coins)
    {
        updatePartial(PlayerRecord.getKey(userId),
                      PlayerRecord.COINS, PlayerRecord.COINS.plus(coins));
    }

    /**
     * Consumes the specified number of coins from the specified player.
     *
     * @return true if the coins were consumed, false if the player did not have sufficient coins
     * (or did not exist).
     */
    public boolean consumeCoins (int userId, int coins)
    {
        return updatePartial(PlayerRecord.class,
                             new Where(Ops.and(PlayerRecord.USER_ID.eq(userId),
                                               PlayerRecord.COINS.greaterEq(coins))),
                             PlayerRecord.getKey(userId),
                             PlayerRecord.COINS, PlayerRecord.COINS.minus(coins)) == 1;
    }

    /**
     * Adds to the specified player's free flip count.
     */
    public void grantFreeFlips (int userId, float flips)
    {
        updatePartial(PlayerRecord.getKey(userId),
                      PlayerRecord.FREE_FLIPS, PlayerRecord.FREE_FLIPS.plus(flips));
    }

    /**
     * Consumes a free flip for the specified player as long as they have at least one free flip
     * available.
     *
     * @return true if the flip was consumed, false if they lacked at least one free flip (or the
     * player didn't exist).
     */
    public boolean consumeFreeFlip (int userId)
    {
        return updatePartial(PlayerRecord.class,
                             new Where(Ops.and(PlayerRecord.USER_ID.eq(userId),
                                               PlayerRecord.FREE_FLIPS.greaterEq(1))),
                             PlayerRecord.getKey(userId),
                             PlayerRecord.FREE_FLIPS, PlayerRecord.FREE_FLIPS.minus(1)) == 1;
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(PlayerRecord.class);
        classes.add(WishRecord.class);
    }
}
