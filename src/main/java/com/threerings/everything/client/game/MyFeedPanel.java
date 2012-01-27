//
// $Id$

package com.threerings.everything.client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.rpc.EverythingService;
import com.threerings.everything.rpc.GameService;
import com.threerings.everything.rpc.GameServiceAsync;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.ThingCard;
import com.threerings.everything.data.SlotStatus;

import com.threerings.everything.client.ui.XFBML;
import com.threerings.everything.client.util.Context;
import com.threerings.everything.client.util.PopupCallback;

/**
 * Displays the current user's feed.
 */
public class MyFeedPanel extends FeedPanel<EverythingService.FeedResult>
{
    public MyFeedPanel (Context ctx)
    {
        super(ctx);
        _everysvc.getRecentFeed(createCallback());
    }

    @Override // from DataPanel
    protected void init (EverythingService.FeedResult result)
    {
        if (result.recruitGifts.size() > 0) {
            add(Widgets.newLabel("Send Free Gifts to your friends!", "Title", "machine"));
            add(Widgets.newLabel("More players means more things!"));
            FluentTable cards = new FluentTable(0, 0);
            int col = 0;
            for (Card card : result.recruitGifts) {
                SlotView slot = new SlotView();
                if (card == null) { // already gifted
                    slot.status.update(SlotStatus.RECRUIT_GIFTED);
                } else {
                    slot.status.update(SlotStatus.FLIPPED);
                    slot.setCard(_ctx, card.toThingCard(), false,
                        CardPopup.recruitGiftClick(_ctx, card, slot.status));
                }
                cards.at(0, col).setWidget(slot);
                col++;
            }
            add(cards);
        }

        if (!result.gifts.isEmpty()) {
            add(Widgets.newLabel("Unopened Gifts", "Title", "machine"));
            FluentTable cards = new FluentTable(0, 0);
            int row = 0, col = 0;
            for (final ThingCard card : result.gifts) {
                final SlotView slot = new SlotView();
                slot.setCard(_ctx, card, true, new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        openGift(card, slot);
                    }
                });
                cards.at(row, col).setWidget(slot);
                if (++col == SeriesPanel.COLUMNS) {
                    row++;
                    col = 0;
                }
            }
            add(cards);
        }

//         List<FeedItem> highlights = new ArrayList<FeedItem>();
//         for (FeedItem item : result.items) {
//             if (item.type == FeedItem.Type.COMMENT) {
//                 highlights.add(item);
//             }
//         }
//         if (!highlights.isEmpty()) {
//             add(Widgets.newLabel("Highlights", "Title", "machine"));
//             while (highlights.size() > 0) {
//                 add(formatItem(highlights.remove(0), highlights, Mode.HIGHLIGHT));
//             }
//             add(Widgets.newShim(5, 5));
//         }

        add(Widgets.newLabel("Recent Happenings", "Title", "machine"));
        while (!result.items.isEmpty()) {
            add(formatItem(result.items.remove(0), result.items, Mode.NORMAL));
        }

        XFBML.parse(this);
    }

    protected void openGift (final ThingCard card, final SlotView slot)
    {
        _gamesvc.openGift(card.thingId, card.received,
                          new PopupCallback<GameService.GiftResult>(slot) {
            public void onSuccess (GameService.GiftResult result) {
                // display the flipped card in the grid
                slot.setCard(_ctx, result.card.toThingCard(), false, null);
                // display the card big and fancy and allow them to gift it or cash it in
                CardPopup.display(_ctx, result, slot.status, slot, result.message);
            }
        });
    }

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
