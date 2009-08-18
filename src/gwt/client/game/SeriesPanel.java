//
// $Id$

package client.game;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickHandler;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.util.Value;

import com.threerings.everything.data.CardIdent;
import com.threerings.everything.data.Series;
import com.threerings.everything.data.ThingCard;

import client.util.Context;

/**
 * Displays thumbnails for all cards in a player's series.
 */
public class SeriesPanel extends FluentTable
{
    public static final int COLUMNS = 5;

    public SeriesPanel (Context ctx, int ownerId, final Series series, final Value<Integer> owned)
    {
        super(5, 0, "series");
        add().setText(series.name, "Title").setColSpan(COLUMNS);
        for (int ii = 0; ii < series.things.length; ii++) {
            final FluentTable.Cell cell = at(ii/COLUMNS+1, ii%COLUMNS);
            final ThingCard card = series.things[ii];
            Value<String> status = new Value<String>("");
            status.addListener(new Value.Listener<String>() {
                public void valueChanged (String status) {
                    cell.setText(status);
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
            ClickHandler onClick = (card == null) ? null : CardPopup.onClick(
                ctx, new CardIdent(ownerId, card.thingId, card.received), status);
            cell.setWidget(new ThingCardView(ctx, card, onClick)).alignCenter().alignMiddle();
        }
    }
}
