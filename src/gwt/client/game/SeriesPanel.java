//
// $Id$

package client.game;

import java.util.HashSet;
import java.util.Set;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.util.Value;

import com.threerings.everything.data.Series;
import com.threerings.everything.data.SlotStatus;
import com.threerings.everything.data.ThingCard;

import client.ui.LikeWidget;
import client.util.Context;

/**
 * Displays thumbnails for all cards in a player's series.
 */
public class SeriesPanel extends FluentTable
{
    public static final int COLUMNS = 5;

    public SeriesPanel (Context ctx, int ownerId, final Series series, final Value<Integer> owned)
    {
        super(0, 0, "series");

        Value<Boolean> liked = Value.create(series.liked);

        add().setWidget(new LikeWidget(series.categoryId, liked)).setColSpan(COLUMNS);
        add().setText(series.name, "Title").setColSpan(COLUMNS);
        for (int ii = 0; ii < series.things.length; ii++) {
            final SlotView slot = new SlotView();
            final ThingCard card = series.things[ii];
            slot.status.addListener(new Value.Listener<SlotStatus>() {
                public void valueChanged (SlotStatus status) {
                    // this card was sold or gifted, so update our owned count
                    Set<Integer> ids = new HashSet<Integer>();
                    for (ThingCard tcard : series.things) {
                        if (tcard != null && tcard != card) {
                            ids.add(tcard.thingId);
                        }
                    }
                    owned.update(ids.size());
                }
            });
            slot.setCard(ctx, card, ownerId, false, liked, null);
            setWidget(ii/COLUMNS+2, ii%COLUMNS, slot);
        }
    }
}
