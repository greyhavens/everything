//
// $Id$

package client.game;

import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FluentTable;
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
            _header = new FluentTable(0, 0, "Header");
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

            _header.at(0, 0).setWidget(XFBML.newProfilePic(coll.owner.facebookId), "Padded").
                setRowSpan(3);
            _header.at(0, 1).setText(coll.owner.toString() + "'s Collection", "Title", "machine");
            _header.at(1, 0).setWidget(links, "machine");
            _header.at(2, 0).setText("");
            _header.at(0, 2).setText("Total things:", "Padded", "right", "handwriting").
                right().setWidget(ValueLabel.create(_cards), "right", "handwriting");
            _header.at(1, 1).setText("Total series:", "Padded", "right", "handwriting").
                right().setWidget(ValueLabel.create(_series), "right", "handwriting");
            _header.at(2, 1).setText("Completed:", "Padded", "right", "handwriting").
                right().setWidget(ValueLabel.create(_completed), "right", "handwriting");
            XFBML.parse(this);
        }
        _coll = coll;

        // determine which series is selected, if any
        String catname = null, subcatname = null, seriesname = null;
        if (_seriesId > 0) {
            for (Map.Entry<String, Map<String, List<SeriesCard>>> cat : coll.series.entrySet()) {
                for (Map.Entry<String, List<SeriesCard>> subcat : cat.getValue().entrySet()) {
                    for (final SeriesCard card : subcat.getValue()) {
                        if (_seriesId == card.categoryId) {
                            catname = cat.getKey();
                            subcatname = subcat.getKey();
                            seriesname = card.name;
                            break;
                        }
                    }
                }
            }
        }

        // now generate our fancy display
        showTaxonomy(catname, subcatname, seriesname);
    }

    protected void showTaxonomy (final String selcat, final String selsubcat, String selseries)
    {
        if (_taxon != null) {
            remove(_taxon);
        }
        if (_panel != null) {
            remove(_panel);
            _panel = null;
        }
        add(_taxon = new FluentTable(5, 0, "Taxonomy", "handwriting"));

        // first render the categories and grab the target subcategory
        Map<String, List<SeriesCard>> subcats = null;
        int catrow = -1, row = 0;
        for (Map.Entry<String, Map<String, List<SeriesCard>>> cat : _coll.series.entrySet()) {
            final String catname = cat.getKey();
            if (catname.equals(selcat)) {
                catrow = row;
                subcats = cat.getValue();
                _taxon.setText(row++, 0, catname);
            } else {
                _taxon.setWidget(row++, 0, Widgets.newActionLabel(catname, new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        showTaxonomy(catname, null, null);
                    }
                }));
            }
        }
        if (subcats == null) {
            return;
        }

        // now render the subcategories and grab the target series
        List<SeriesCard> series = null;
        int subcatrow = -1;
        row = 0;
        for (Map.Entry<String, List<SeriesCard>> subcat : subcats.entrySet()) {
            final String subcatname = subcat.getKey();
            if (subcatname.equals(selsubcat)) {
                subcatrow = row;
                series = subcat.getValue();
                _taxon.setText(row++, 1, subcatname);
            } else {
                _taxon.setWidget(row++, 1, Widgets.newActionLabel(subcatname, new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        showTaxonomy(selcat, subcatname, null);
                    }
                }));
            }
        }
        if (series == null) {
            return;
        }

        // finally render the series and grab the target series
        row = 0;
        for (final SeriesCard card : series) {
            Widget name;
            if (card.name.equals(selseries)) {
                //name = Args.createInlink(card.name, Page.BROWSE, coll.owner.userId);
                name = Widgets.newLabel(card.name);
            } else {
                name = Args.createInlink(
                    card.name, Page.BROWSE, _coll.owner.userId, card.categoryId);
            }
            if (card.owned == card.things) {
                name.addStyleName("Complete");
            }
            _taxon.setWidget(row, 2, name);

            Value<Integer> owned = new Value<Integer>(card.owned) {
                public void update (Integer value) {
                    super.update(value);
                    card.owned = value;
                    _cards.update(_coll.countCards());
                    _series.update(_coll.countSeries());
                    _completed.update(_coll.countCompletedSeries());
                }
            };
            _taxon.setWidget(row++, 3, new ValueLabel<Integer>("Held", owned) {
                protected String getText (Integer owned) {
                    return " " + owned + " of " + card.things;
                }
            });

            if (_seriesId == card.categoryId) {
                _panel = new SeriesPanel(_ctx, _coll.owner.userId, card.categoryId, owned);
            }
        }

        if (_panel != null) {
            add(_panel);
        }
    }

    protected int _seriesId;
    protected PlayerCollection _coll;
    protected FluentTable _header, _taxon;
    protected SeriesPanel _panel;
    protected Value<Integer> _cards, _series, _completed;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
