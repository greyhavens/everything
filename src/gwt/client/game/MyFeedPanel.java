//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.ThingCard;
import com.threerings.everything.data.SlotStatus;

import client.ui.XFBML;
import client.util.Context;
import client.util.PopupCallback;

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
        if (result.recruitGift != null) {
            add(Widgets.newLabel("Send a Free Gift to your friends!", "Title"));
            add(Widgets.newLabel("More players means more things!"));
            SlotView recruitSlot = new SlotView();
            if (result.recruitGift.thing == null) { // already gifted
                recruitSlot.status.update(SlotStatus.RECRUIT_GIFTED);
            } else {
                recruitSlot.status.update(SlotStatus.FLIPPED);
                ClickHandler clickHandler =
                    CardPopup.recruitGiftClick(_ctx, result.recruitGift, recruitSlot.status);
                recruitSlot.setCard(_ctx, result.recruitGift.toThingCard(), false, clickHandler);
            }
            add(recruitSlot);
        }

        if (!result.gifts.isEmpty()) {
            add(Widgets.newLabel("Unopened Gifts", "Title"));
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
//             add(Widgets.newLabel("Highlights", "Title"));
//             while (highlights.size() > 0) {
//                 add(formatItem(highlights.remove(0), highlights, Mode.HIGHLIGHT));
//             }
//             add(Widgets.newShim(5, 5));
//         }

        add(Widgets.newLabel("Recent Happenings", "Title"));
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
