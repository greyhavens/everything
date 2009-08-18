//
// $Id$

package client.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.RevealPanel;
import com.threerings.gwt.ui.ValueLabel;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Console;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.PlayerCollection;
import com.threerings.everything.data.SeriesCard;

import client.ui.DataPanel;
import client.ui.XFBML;
import client.ui.lines.LineImages;
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
        add(_taxon = new FluentTable(0, 0, "Taxonomy", "handwriting"));

        // first render the categories and grab the target subcategory
        Map<String, List<SeriesCard>> subcats = null;
        List<SeriesCard> series = null;
        int catrow = -1, subcatrow = -1, row = 0;
        for (Map.Entry<String, Map<String, List<SeriesCard>>> cat : _coll.series.entrySet()) {
            final String catname = cat.getKey();

            // if it's not the selected category, render the subcategories in compact form
            if (!catname.equals(selcat)) {
                Widget catlink = Widgets.newActionLabel(catname, new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        showTaxonomy(catname, null, null);
                    }
                });
                _taxon.at(row++, 0).setWidget(catlink).
                    right().setWidget(createLine(false, false, true, true)).
                    right().setWidget(createCatSum(cat.getKey(), cat.getValue())).setColSpan(3);
                continue;
            }

            // if it is the selected category, render the subcats in expanded form
            _taxon.at(row, 0).setText(catname, "Selected");
            catrow = row;
            subcats = cat.getValue();
            for (Map.Entry<String, List<SeriesCard>> subcat : subcats.entrySet()) {
                final String subcatname = subcat.getKey();
                _taxon.setWidget(row, 1, createLine(row > catrow, row < catrow+subcats.size()-1,
                                                    row == catrow, true));
                if (subcatname.equals(selsubcat)) {
                    subcatrow = row;
                    series = subcat.getValue();
                    _taxon.at(row, 2).setText(subcatname, "Selected");
                } else {
                    _taxon.setWidget(row, 2, Widgets.newActionLabel(subcatname, new ClickHandler() {
                        public void onClick (ClickEvent event) {
                            showTaxonomy(selcat, subcatname, null);
                        }
                    }));
                }
                row++;
            }
        }

        SeriesPanel panel = null;
        if (series != null) {
            // render our connecting lines between subcategory and series
            for (int lrow = catrow, last = Math.max(subcatrow, catrow+series.size()-1);
                 lrow <= last; lrow++) {
                _taxon.setWidget(lrow, 3, createLine(lrow > catrow, lrow < last, lrow == subcatrow,
                                                     lrow < catrow+series.size()));
            }

            // finally render the series and grab the target series
            row = catrow;
            for (final SeriesCard card : series) {
                Widget name;
                if (card.name.equals(selseries) ||
                    (selseries == null && card.categoryId == _seriesId)) {
                    name = Widgets.newInlineLabel(card.name, "Selected");
                } else {
                    name = Args.createInlink(
                        card.name, Page.BROWSE, _coll.owner.userId, card.categoryId);
                }

                Value<Integer> owned = new Value<Integer>(card.owned) {
                    public void update (Integer value) {
                        super.update(value);
                        card.owned = value;
                        _cards.update(_coll.countCards());
                        _series.update(_coll.countSeries());
                        _completed.update(_coll.countCompletedSeries());
                    }
                };
                ValueLabel<Integer> olabel = new ValueLabel<Integer>("Held", owned) {
                    protected String getText (Integer owned) {
                        return " " + owned + " of " + card.things;
                    }
                };
                olabel.addStyleName((card.owned == card.things) ? "Complete" : "Incomplete");

                _taxon.at(row++, 4).setWidget(
                    Widgets.newFlowPanel(name, Widgets.newInlineLabel(" "), olabel));

                if (card.name.equals(selseries)) {
                    panel = new SeriesPanel(_ctx, _coll.owner.userId, card.categoryId, owned);
                }
            }
        }

        Command onHidden = null;
        if (panel != null) {
            final RevealPanel spanel = new RevealPanel(panel);
            onHidden = new Command() {
                public void execute () {
                    insert(spanel, getWidgetIndex(_taxon));
                    spanel.reveal(null);
                    _spanel = spanel;
                }
            };
        }
        if (_spanel != null && (panel != null || _seriesId == 0)) {
            _spanel.hideAndRemove(onHidden);
            _spanel = null;
        } else if (onHidden != null) {
            onHidden.execute();
        }
    }

    protected FlowPanel createCatSum (final String catname, Map<String, List<SeriesCard>> subcats)
    {
        FlowPanel links = new FlowPanel();
        for (final Map.Entry<String, List<SeriesCard>> subcat : subcats.entrySet()) {
            if (links.getWidgetCount() > 0) {
                links.add(Widgets.newHTML("&nbsp;&nbsp; ", "inline"));
            }
            links.add(Widgets.newActionLabel(subcat.getKey(), "inline", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    showTaxonomy(catname, subcat.getKey(), null);
                }
            }));
        }
        return links;
    }

    protected Image createLine (boolean up, boolean down, boolean left, boolean right)
    {
        String key = "";
        if (up) key += "U";
        if (down) key += "D";
        if (left) key += "L";
        if (right) key += "R";
        AbstractImagePrototype img = _linemap.get(key);
        if (img == null) {
            Console.log("Missing image " + key);
            img = _linemap.get("UDLR");
        }
        return img.createImage();
    }

    protected int _seriesId;
    protected PlayerCollection _coll;
    protected FluentTable _header, _taxon;
    protected RevealPanel _spanel;
    protected Value<Integer> _cards, _series, _completed;

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);

    protected static final LineImages _lines = GWT.create(LineImages.class);
    protected static Map<String, AbstractImagePrototype> _linemap =
        new HashMap<String, AbstractImagePrototype>();
    static {
        _linemap.put("DL", _lines.downleft());
        _linemap.put("DR", _lines.downright());
        _linemap.put("ULR", _lines.upleftright());
        _linemap.put("DLR", _lines.downleftright());
        _linemap.put("UDL", _lines.updownleft());
        _linemap.put("UDR", _lines.updownright());
        _linemap.put("UL", _lines.upleft());
        _linemap.put("UR", _lines.upright());
        _linemap.put("LR", _lines.leftright());
        _linemap.put("UD", _lines.updown());
        _linemap.put("UDLR", _lines.full());
    };
}
