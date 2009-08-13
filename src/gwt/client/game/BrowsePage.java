//
// $Id$

package client.game;

import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
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
        if (_header == null) {
            _header = new SmartTable("Header", 0, 0);
        }
        if (!_header.isAttached()) {
            add(_header);
        }
        if (_coll == null || _coll.owner.userId != coll.owner.userId) {
            _cards = Value.create(coll.countCards());
            _series = Value.create(coll.countSeries());
            _completed = Value.create(coll.countCompletedSeries());

            final Label feed = Widgets.newLabel("View Feed");
            Widgets.makeActionLabel(feed, new ClickHandler() {
                public void onClick (ClickEvent event) {
                    if (_taxon.isAttached()) {
                        remove(_taxon);
                        feed.setText("View Collection");
                        if (_feed == null) {
                            _feed = new UserFeedPanel(_ctx, _coll.owner.userId);
                        }
                        add(_feed);
                    } else {
                        remove(_feed);
                        feed.setText("View Feed");
                        add(_taxon);
                    }
                }
                protected FeedPanel _feed;
            });
            Widget links = _ctx.getMe().equals(coll.owner) ? Widgets.newRow("Links", feed) :
                Widgets.newRow("Links", feed, Args.createMessageAnchor(coll.owner));

            _header.setWidget(0, 0, XFBML.newProfilePic(coll.owner.facebookId), 1, "Padded");
            _header.getFlexCellFormatter().setRowSpan(0, 0, 3);
            _header.setText(0, 1, coll.owner.toString() + "'s Collection", 1, "Title", "machine");
            _header.setWidget(1, 0, links, 1, "machine");
            _header.setText(2, 0, "");
            _header.setText(0, 2, "Total things:", 1, "Padded", "right", "handwriting");
            _header.setText(1, 1, "Total series:", 1, "Padded", "right", "handwriting");
            _header.setText(2, 1, "Completed:", 1, "Padded", "right", "handwriting");
            _header.setWidget(0, 3, ValueLabel.create(_cards), 1, "right", "handwriting");
            _header.setWidget(1, 2, ValueLabel.create(_series), 1, "right", "handwriting");
            _header.setWidget(2, 2, ValueLabel.create(_completed), 1, "right", "handwriting");
            XFBML.parse(this);
        }
        _coll = coll;

        if (_taxon != null) {
            remove(_taxon);
        }
        _taxon = new SmartTable("Taxonomy", 5, 0);
        _taxon.addStyleName("handwriting");
        for (Map.Entry<String, Map<String, List<SeriesCard>>> cat : coll.series.entrySet()) {
            String catname = cat.getKey();
            for (Map.Entry<String, List<SeriesCard>> subcat : cat.getValue().entrySet()) {
                String subcatname = subcat.getKey();
                final int row = _taxon.addText(catname, 1);
                catname = ""; // subsequent rows don't repeat the same category
                _taxon.setText(row, 1, subcatname);
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
                            _cards.update(coll.countCards());
                            _series.update(coll.countSeries());
                            _completed.update(coll.countCompletedSeries());
                        }
                    };
                    cards.add(new ValueLabel<Integer>("Held", owned) {
                        protected String getText (Integer owned) {
                            return " " + owned + " of " + card.things;
                        }
                    });
                    if (_seriesId == card.categoryId) {
                        _taxon.addWidget(new SeriesPanel(_ctx, coll.owner.userId,
                                                         card.categoryId, owned), 3);
                    }
                }
                _taxon.setWidget(row, 2, cards);
                _taxon.getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_TOP);
                _taxon.getFlexCellFormatter().setVerticalAlignment(row, 1, HasAlignment.ALIGN_TOP);
            }
        }
        add(_taxon);
    }

    protected int _seriesId;
    protected PlayerCollection _coll;
    protected SmartTable _header, _taxon;
    protected Value<Integer> _cards, _series, _completed;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
