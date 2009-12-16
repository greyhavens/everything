//
// $Id$

package com.threerings.everything.server.persist;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DateFuncs;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Key;
import com.samskivert.depot.KeySet;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.util.Calendars;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.threerings.everything.client.GameCodes;
import com.threerings.everything.data.FeedItem;
import com.threerings.everything.data.FriendStatus;
import com.threerings.everything.data.Player;
import com.threerings.everything.data.PlayerName;

import static com.threerings.everything.Log.log;

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
     * Loads and returns the names for the supplied set of players, mapped by user id.
     */
    public List<PlayerRecord> loadPlayers (Collection<Integer> userIds)
    {
        return findAll(PlayerRecord.class, new Where(PlayerRecord.USER_ID.in(userIds)));
    }

    /**
     * Loads and returns records for players whose last session falls during the current hour
     * precisely two, four or six days in the past.
     */
    public List<PlayerRecord> loadIdlePlayers ()
    {
        int hour = Calendars.now().get(Calendar.HOUR_OF_DAY);
        return findAll(PlayerRecord.class,
                       new Where(Ops.or(fromTo(2, hour), fromTo(4, hour), fromTo(6, hour))));
    }

    /**
     * Loads and returns the names for the supplied set of players, mapped by user id.
     */
    public IntMap<PlayerName> loadPlayerNames (Collection<Integer> userIds)
    {
        IntMap<PlayerName> names = IntMaps.newHashIntMap();
        for (PlayerRecord prec : loadPlayers(userIds)) {
            names.put(prec.userId, PlayerRecord.TO_NAME.apply(prec));
        }
        return names;
    }

    /**
     * Loads players that have recently joined.
     */
    public Collection<PlayerName> loadRecentPlayers (int count)
    {
        return findAll(PlayerRecord.class, OrderBy.descending(PlayerRecord.USER_ID),
                       new Limit(0, count))
            .map(PlayerRecord.TO_NAME);
    }

    /**
     * Summarizes the number of registrations per day for the specified number of days into the
     * past.
     *
     * @return a map from date to number of players that registered on that date.
     */
    public SortedMap<Date, Integer> summarizeRegis (int sinceDays)
    {
        Timestamp since = Calendars.now().zeroTime().addDays(-sinceDays).toTimestamp();
        SortedMap<Date, Integer> regis = Maps.newTreeMap();
        for (RegiSummaryRecord rec : findAll(RegiSummaryRecord.class,
                                             new Where(PlayerRecord.JOINED.greaterEq(since)),
                                             new GroupBy(RegiSummaryRecord.WHEN))) {
            regis.put(new Date(rec.when.getTime()), rec.count);
        }
        return regis;
    }

    /**
     * Returns up to 1000 players who's birthday has arrived and have not yet received a present.
     * These players will be marked as having received their gift, so be sure to be robust about
     * making use of the returned ids.
     */
    public Collection<PlayerRecord> loadBirthdayPlayers ()
    {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR), today = toDateVal(cal.getTimeInMillis());
        // load up the keys for up to 1000 players whose birthday has arrived
        Where where = new Where(Ops.and(PlayerRecord.BIRTHDATE.lessEq(today),
                                        PlayerRecord.BIRTHDATE.notEq(0),
                                        PlayerRecord.LAST_GIFT_YEAR.lessThan(year)));
        Collection<PlayerRecord> players = findAll(PlayerRecord.class, where, new Limit(0, 1000));
        List<Key<PlayerRecord>> keys = Lists.newArrayListWithCapacity(players.size());
        // mark those players as having been gifted
        for (PlayerRecord prec : players) {
            keys.add(PlayerRecord.getKey(prec.userId));
        }
        KeySet<PlayerRecord> keyset = KeySet.newKeySet(PlayerRecord.class, keys);
        updatePartial(PlayerRecord.class, keyset, keyset, PlayerRecord.LAST_GIFT_YEAR, year);
        return players;
    }

    /**
     * Searches for players by first or last name.
     */
    public Collection<PlayerName> findPlayers (String query)
    {
        // TODO: none of this is indexed nor is it case insensitive, change to full text
        return findAll(PlayerRecord.class, new Where(Ops.or(PlayerRecord.NAME.eq(query),
                                                            PlayerRecord.SURNAME.eq(query))))
            .map(PlayerRecord.TO_NAME);
    }

    /**
     * Creates, inserts and returns a new player record.
     */
    public PlayerRecord createPlayer (int userId, long facebookId, String name, String surname,
                                      long birthday, String timezone)
    {
        PlayerRecord record = new PlayerRecord();
        record.userId = userId;
        record.facebookId = facebookId;
        record.name = name;
        record.surname = surname;
        if (birthday != 0L) {
            record.birthdate = toDateVal(birthday);
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            cal.setTimeInMillis(birthday);
            cal.set(Calendar.YEAR, year);
            record.lastGiftYear = year;
            // if their birthday hasn't happened yet this year, back the gift year up a year
            if (cal.getTimeInMillis() >= System.currentTimeMillis()) {
                record.lastGiftYear--;
            }
        }
        record.timezone = timezone;
        record.joined = new Timestamp(System.currentTimeMillis());
        record.lastSession = record.joined;
        record.coins = GameCodes.NEW_USER_FREE_COINS;
        insert(record);
        return record;
    }

    /**
     * Updates the specified user's stored Facebook id.
     */
    public void updateFacebookId (int userId, long facebookId)
    {
        updatePartial(PlayerRecord.getKey(userId), PlayerRecord.FACEBOOK_ID, facebookId);
    }

    /**
     * TEMP: updates player's birthday.
     */
    public void updateBirthday (int userId, long birthday)
    {
        updatePartial(PlayerRecord.getKey(userId), PlayerRecord.BIRTHDATE, toDateVal(birthday));
    }

    /**
     * Updates the specified player's editor status.
     */
    public void updateIsEditor (int userId, boolean isEditor)
    {
        updatePartial(PlayerRecord.getKey(userId), PlayerRecord.IS_EDITOR, isEditor);
    }

    /**
     * Activates or deactivates the specified flag for the specified player.
     */
    public void updateFlag (PlayerRecord player, Player.Flag flag, boolean activate)
    {
        if (activate) {
            updatePartial(PlayerRecord.getKey(player.userId),
                          PlayerRecord.FLAGS, PlayerRecord.FLAGS.bitOr(flag.getMask()));
            player.setFlag(flag);
        } else {
            updatePartial(PlayerRecord.getKey(player.userId),
                          PlayerRecord.FLAGS, PlayerRecord.FLAGS.bitAnd(~flag.getMask()));
            player.clearFlag(flag);
        }
    }

    /**
     * Creates friend mappings for the specified user to the specified set of friends. The mapping
     * will be created in both directions so that as players join Everything old players will be
     * connected to joining friends when the friend joins.
     */
    public void addFriends (int userId, Iterable<Integer> friendIds)
    {
        for (Integer friendId : friendIds) {
            FriendRecord record = new FriendRecord();
            record.userId = userId;
            record.friendId = friendId;
            store(record);
            record = new FriendRecord();
            record.userId = friendId;
            record.friendId = userId;
            store(record);
        }
    }

    /**
     * Remove friend mappings.
     * @return the number of friends removed.
     */
    public int removeFriends (int userId, Collection<Integer> exFriendIds)
    {
        int forward = deleteAll(FriendRecord.class,
            new Where(Ops.and(FriendRecord.USER_ID.eq(userId),
                              FriendRecord.FRIEND_ID.in(exFriendIds))));
        int backward = 0;
        for (Integer friendId : exFriendIds) {
            backward += delete(FriendRecord.getKey(friendId, userId));
        }
        if (forward != backward) {
            log.warning("Removing friend mappings that aren't reciprocal?",
                "forward", forward, "backward", backward);
        }
        return forward;
    }

    /**
     * Loads this status of this user's friends.
     */
    public Collection<FriendStatus> loadFriendStatus (int userId)
    {
        return findAll(PlayerRecord.class,
                       PlayerRecord.USER_ID.join(FriendRecord.FRIEND_ID),
                       new Where(FriendRecord.USER_ID.eq(userId)),
                       OrderBy.descending(PlayerRecord.LAST_SESSION))
            .map(PlayerRecord.TO_FRIEND_STATUS);
    }

    /**
     * Loads the ids of all friends of the specified user.
     */
    public Collection<Integer> loadFriendIds (int userId)
    {
        return findAll(FriendRecord.class, new Where(FriendRecord.USER_ID.eq(userId)))
            .map(new Function<FriendRecord, Integer>() {
                public Integer apply (FriendRecord record) {
                    return record.friendId;
                }
            });
    }

    /**
     * Updates the specified user's last session stamp and grants them free flips earned since
     * their previous session.
     */
    public void recordSession (PlayerRecord player, long sessionStamp, String timezone)
    {
        Map<ColumnExp, Object> updates = Maps.newHashMap();
        updates.put(PlayerRecord.LAST_SESSION, new Timestamp(sessionStamp));
        if (!player.timezone.equals(timezone)) {
            updates.put(PlayerRecord.TIMEZONE, timezone);
            player.timezone = timezone; // and update it in the local record instance
        }
        updatePartial(PlayerRecord.getKey(player.userId), updates);
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
     * Expire any old recruitment gift records.
     */
    public int pruneGiftRecords ()
    {
        // TODO: this was changed to keep the records for a few days because now I
        // determine whether to give recruitment gifts by the behavior on previous days...
        // If that is dumped, we can revert to expiring these records aggressively.
        Timestamp oldness = Calendars.now().addDays(-2).toTimestamp();
        return deleteAll(RecruitGiftRecord.class,
            new Where(RecruitGiftRecord.EXPIRES.lessThan(oldness)));
            //new Where(RecruitGiftRecord.EXPIRES.lessThan(teFuncs.now())));
    }

    /**
     * Loads the RecruitGiftRecord for the specified player.
     */
    public RecruitGiftRecord loadRecruitGifts (int userId)
    {
        return load(RecruitGiftRecord.getKey(userId));
    }

    /**
     * Store newly-generated recruit gifts for the specified player.
     */
    public RecruitGiftRecord storeRecruitGifts (PlayerRecord player, int[] giftIds)
    {
        RecruitGiftRecord recruit = new RecruitGiftRecord();
        recruit.userId = player.userId;
        recruit.giftIds = giftIds;
        recruit.expires = player.calculateNextExpires();
        store(recruit);
        return recruit;
    }

    /**
     * Note that the player used one of their daily recruit gifts.
     *
     * @return true if they did indeed have this gift.
     */
    public boolean noteRecruitGiftSent (int userId, int thingId)
    {
        RecruitGiftRecord rec = loadRecruitGifts(userId);
        if (rec != null) {
            int index = rec.getGiftIndex(thingId);
            if (index != -1) {
                rec.giftIds[index] = 0;
                update(rec);
                return true;
            }
        }
        return false;
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
     * Adds to the specified player's free flip count. Updates the supplied record in memory also.
     */
    public void grantFreeFlips (PlayerRecord record, int flips)
    {
        updatePartial(PlayerRecord.getKey(record.userId),
                      PlayerRecord.FREE_FLIPS, PlayerRecord.FREE_FLIPS.plus(flips));
        record.freeFlips += flips;
    }

    /**
     * Grants the specified number of free flips to every single player in the database.
     */
    public void grantFreeFlipsToEveryone (int flips)
    {
        updatePartial(PlayerRecord.class, new Where(PlayerRecord.USER_ID.greaterThan(0)), null,
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

    /**
     * Set the 'next attractor' time.
     */
    public void setNextAttractor (PlayerRecord record, Timestamp stamp)
    {
        updatePartial(PlayerRecord.getKey(record.userId), PlayerRecord.NEXT_ATTRACTOR, stamp);
        record.nextAttractor = stamp;
    }

    /**
     * Records an item to the specified actor's feed.
     */
    public void recordFeedItem (int actorId, FeedItem.Type type, int targetId, String object)
    {
        FeedItemRecord record = new FeedItemRecord();
        record.actorId = actorId;
        record.when = new Timestamp(System.currentTimeMillis());
        record.type = type;
        record.targetId = targetId;
        record.object = object;
        insert(record);
    }

    /**
     * Returns up to the specified maximum number of feed items for the specified player.
     */
    public Collection<FeedItem> loadRecentFeed (int userId, int maxItems)
    {
        // load up this player's friends and add them to the list of actors
        Set<Integer> actorIds = Sets.newHashSet(loadFriendIds(userId));
        actorIds.add(userId);
        return findAll(FeedItemRecord.class,
                       new Where(FeedItemRecord.ACTOR_ID.in(actorIds)),
                       OrderBy.descending(FeedItemRecord.WHEN),
                       new Limit(0, maxItems))
            .map(FeedItemRecord.TO_FEED_ITEM);
    }

    /**
     * Returns up to the specified maximum number of feed items for which the specified player is
     * the actor.
     */
    public Collection<FeedItem> loadUserFeed (int userId, int maxItems)
    {
        return findAll(FeedItemRecord.class,
                       new Where(FeedItemRecord.ACTOR_ID.eq(userId)),
                       OrderBy.descending(FeedItemRecord.WHEN),
                       new Limit(0, maxItems))
            .map(FeedItemRecord.TO_FEED_ITEM);
    }

    /**
     * Prunes items in the feed older than the specified number of days.
     */
    public int pruneFeed (int days)
    {
        long cutoff = System.currentTimeMillis() - days * 24*60*60*1000L;
        return deleteAll(FeedItemRecord.class,
                         new Where(FeedItemRecord.WHEN.lessThan(new Timestamp(cutoff))), null);
    }

    /** Helper for {@link #loadIdlePlayers}. */
    protected SQLExpression fromTo (int daysAgo, int hour)
    {
        Timestamp from = Calendars.now().zeroTime().addDays(-daysAgo).addHours(hour).toTimestamp();
        Timestamp to = Calendars.now().zeroTime().addDays(-daysAgo).addHours(hour+1).toTimestamp();
        return Ops.and(PlayerRecord.LAST_SESSION.greaterEq(from),
                       PlayerRecord.LAST_SESSION.lessThan(to));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(FeedItemRecord.class);
        classes.add(FriendRecord.class);
        classes.add(PlayerRecord.class);
        classes.add(WishRecord.class);
        classes.add(RecruitGiftRecord.class);
    }

    /**
     * Converts a date to MMDD format.
     */
    protected static int toDateVal (long when)
    {
        Calendar cal = Calendars.at(when).asCalendar();
        return (cal.get(Calendar.MONTH)+1) * 100 + cal.get(Calendar.DAY_OF_MONTH);
    }
}
