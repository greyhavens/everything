//
// $Id$

package client.game;

import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.ValueLabel;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.PlayerCollection;
import com.threerings.everything.data.SeriesCard;

import client.ui.DataPanel;
import client.ui.XFBML;
import client.util.Args;
import client.util.Context;
import client.util.Page;
import client.util.PanelCallback;

/**
 * Displays a player's collection.
 */
public class BrowsePage extends DataPanel<PlayerCollection>
{
    public BrowsePage (Context ctx)
    {
        super(ctx, "page", "browse");
    }

    public void setArgs (int ownerId, int seriesId)
    {
        _seriesId = seriesId;
        if (_coll == null || _coll.owner.userId != ownerId) {
            _gamesvc.getCollection(ownerId, createCallback());
        } else {
            init(_coll);
        }
    }

    @Override // from DataPanel
    protected void init (final PlayerCollection coll)
    {
        _coll = coll;

        if (_header == null) {
            _header = new SmartTable("machine", 5, 0);
            _header.setWidget(0, 0, XFBML.newProfilePic(coll.owner.facebookId));
            Widget title = Widgets.newLabel(coll.owner.toString() + "'s Collection", "Title");
            if (!_ctx.getMe().equals(coll.owner)) {
                title = Widgets.newFlowPanel(title, Args.createMessageAnchor(coll.owner));
            }
            _header.setWidget(0, 1, title, 2, "Header");
            add(_header);
            XFBML.parse(this);
        }

        if (_table != null) {
            remove(_table);
        }
        _table = new SmartTable("Taxonomy", 5, 0);
        _table.addStyleName("handwriting");
        for (Map.Entry<String, Map<String, List<SeriesCard>>> cat : coll.series.entrySet()) {
            String catname = cat.getKey();
            for (Map.Entry<String, List<SeriesCard>> subcat : cat.getValue().entrySet()) {
                String subcatname = subcat.getKey();
                final int row = _table.addText(catname, 1);
                catname = ""; // subsequent rows don't repeat the same category
                _table.setText(row, 1, subcatname);
                FlowPanel cards = new FlowPanel();
                for (final SeriesCard card : subcat.getValue()) {
                    if (cards.getWidgetCount() > 0) {
                        cards.add(Widgets.newInlineLabel(" "));
                    }
                    Widget name;
                    if (_seriesId == card.categoryId) {
                        name = Args.createInlink(card.name, Page.BROWSE, coll.owner.userId);
                    } else {
                        name = Args.createInlink(
                            card.name, Page.BROWSE, coll.owner.userId, card.categoryId);
                    }
                    if (card.owned == card.things) {
                        name.addStyleName("Complete");
                    }
                    cards.add(name);
                    Value<Integer> owned = new Value<Integer>(card.owned) {
                        public void update (Integer value) {
                            super.update(value);
                            card.owned = value;
                        }
                    };
                    cards.add(new ValueLabel<Integer>("Held", owned) {
                        protected String getText (Integer owned) {
                            return " " + owned + " of " + card.things;
                        }
                    });
                    if (_seriesId == card.categoryId) {
                        _table.addWidget(
                            new SeriesPanel(_ctx, coll.owner.userId, card.categoryId, owned), 3);
                    }
                }
                _table.setWidget(row, 2, cards);
                _table.getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_TOP);
                _table.getFlexCellFormatter().setVerticalAlignment(row, 1, HasAlignment.ALIGN_TOP);
            }
        }
        add(_table);
    }

    protected int _seriesId;
    protected PlayerCollection _coll;
    protected SmartTable _header, _table;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
