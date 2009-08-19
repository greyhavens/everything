//
// $Id$

package client.game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FX;
import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.ValueLabel;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Console;
import com.threerings.gwt.util.StringUtil;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.GameService;
import com.threerings.everything.client.GameServiceAsync;
import com.threerings.everything.data.PlayerCollection;
import com.threerings.everything.data.Series;
import com.threerings.everything.data.SeriesCard;

import client.ui.DataPanel;
import client.ui.XFBML;
import client.ui.lines.LineImages;
import client.util.Args;
import client.util.Context;
import client.util.Page;
import client.util.PopupCallback;

/**
 * Displays a player's collection.
 */
public class BrowsePage extends DataPanel<PlayerCollection>
{
    public BrowsePage (Context ctx)
    {
        super(ctx, "page", "browse");
    }

    public void setArgs (int ownerId, int seriesId, String selcat)
    {
        _seriesId = seriesId;
        _selcat = selcat;
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
        String catname = _selcat, subcatname = null;
        if (_seriesId > 0) {
            for (Map.Entry<String, Map<String, List<SeriesCard>>> cat : coll.series.entrySet()) {
                for (Map.Entry<String, List<SeriesCard>> subcat : cat.getValue().entrySet()) {
                    for (final SeriesCard card : subcat.getValue()) {
                        if (_seriesId == card.categoryId) {
                            catname = cat.getKey();
                            subcatname = subcat.getKey();
                            break;
                        }
                    }
                }
            }
        }

        // now generate our fancy display
        setTaxonomy(StringUtil.isBlank(catname) ?
                    createOverview() : createTaxonomy(catname, subcatname));
    }

    protected void setTaxonomy (Widget taxon)
    {
        if (_taxon != null) {
            remove(_taxon);
        }
        insert(_taxon = taxon, 1);

        if (_seriesId == 0 && _clearSeries != null) {
            _clearSeries.execute();
            _clearSeries = null;
        }
    }

    protected Widget createOverview ()
    {
        FluentTable taxon = new FluentTable(0, 0, "Taxonomy", "handwriting");
        for (Map.Entry<String, Map<String, List<SeriesCard>>> cat : _coll.series.entrySet()) {
            int catrow = taxon.add().setText(cat.getKey(), "nowrap").alignRight().row, row = catrow;
            for (Map.Entry<String, List<SeriesCard>> subcat : cat.getValue().entrySet()) {
                taxon.at(row, 1).setWidget(createLine(row > catrow,
                                                      row < catrow+cat.getValue().size()-1,
                                                      row == catrow, true)).
                    right().setText(subcat.getKey(), "nowrap").
                    right().setWidget(createLine(false, false, true, true)).
                    right().setWidget(seriesLinks(subcat.getValue()));
                row++;
            }
        }
        return taxon;
    }

    protected FlowPanel seriesLinks (List<SeriesCard> cards)
    {
        FlowPanel links = new FlowPanel();
        for (SeriesCard card : cards) {
            if (links.getWidgetCount() > 0) {
                links.add(Widgets.newHTML("&nbsp; ", "inline"));
            }
            Widget link = Args.createInlink(
                card.name, Page.BROWSE, _coll.owner.userId, card.categoryId);
            if (card.owned == card.things) {
                link.addStyleName("Complete");
            }
            links.add(link);
        }
        return links;
    }

    protected Widget createTaxonomy (final String selcat, String selsubcat)
    {
        // add the unselected categories
        FluentTable cats = new FluentTable(5, 0, "Codons", "machine");
        int col = 0;
        cats.at(0, col++).setWidget(Args.createLink("All", Page.BROWSE, _coll.owner.userId));
        Map<String, String> codons = toCodons(_coll.series.keySet());
        for (final String catname : _coll.series.keySet()) {
            Widget name = catname.equals(selcat) ? Widgets.newLabel(codons.get(catname)) :
                Args.createLink(codons.get(catname), Page.BROWSE, _coll.owner.userId, catname);
            name.setTitle(catname);
            cats.at(0, col++).setWidget(name);
        }

        // render the selected category and subcategories
        FluentTable taxon = new FluentTable(0, 0, "Taxonomy", "handwriting");
        Map<String, List<SeriesCard>> cat = _coll.series.get(selcat);
        if (cat != null) {
            int catrow = taxon.add().setText(selcat, "nowrap").row, row = catrow;
            for (Map.Entry<String, List<SeriesCard>> subcat : cat.entrySet()) {
                final String subcatname = subcat.getKey();
                taxon.setWidget(row, 1, createLine(row > catrow, row < catrow+cat.size()-1,
                                                   row == catrow, true));
                if (selsubcat == null) {
                    selsubcat = subcatname;
                }
                if (subcatname.equals(selsubcat)) {
                    addSeries(taxon, catrow, row, subcat.getValue());
                    taxon.at(row, 2).setText(subcatname, "nowrap");
                } else {
                    taxon.at(row, 2).setWidget(
                        Widgets.newActionLabel(subcatname, new ClickHandler() {
                            public void onClick (ClickEvent event) {
                                setTaxonomy(createTaxonomy(selcat, subcatname));
                            }
                        }), "nowrap");
                }
                row++;
            }
        }

        return Widgets.newFlowPanel(cats, taxon);
    }

    protected void addSeries (FluentTable taxon, int catrow, int subcatrow, List<SeriesCard> series)
    {
        int row = Math.max(catrow, subcatrow-series.size()+1);

        // render our connecting lines between subcategory and series
        for (int lrow = row; lrow < row+series.size(); lrow++) {
            taxon.setWidget(lrow, 3, createLine(lrow > row, lrow < row+series.size()-1,
                                                lrow == subcatrow, true));
        }

        // finally render the series and grab the target series
        for (final SeriesCard card : series) {
            Value<Integer> owned = new Value<Integer>(card.owned) {
                public void update (Integer value) {
                    super.update(value);
                    card.owned = value;
                    _cards.update(_coll.countCards());
                    _series.update(_coll.countSeries());
                    _completed.update(_coll.countCompletedSeries());
                }
            };

            Widget name;
            if (card.categoryId == _seriesId) {
                name = Widgets.newInlineLabel(card.name);
                displaySeries(card, owned);
            } else {
                name = Args.createInlink(
                    card.name, Page.BROWSE, _coll.owner.userId, card.categoryId);
            }
            ValueLabel<Integer> olabel = new ValueLabel<Integer>("Held", owned) {
                protected String getText (Integer owned) {
                    return " " + owned + " of " + card.things;
                }
            };
            taxon.at(row++, 4).setWidgets(name, Widgets.newInlineLabel(" "), olabel).
                setStyles("Leaf", (card.owned == card.things) ? "Complete" : "Incomplete");
        }
    }

    protected void displaySeries (SeriesCard card, final Value<Integer> owned)
    {
        int rows = card.things / SeriesPanel.COLUMNS +
            ((card.things % SeriesPanel.COLUMNS == 0) ? 0 : 1);
        final int animTime = (26 + rows * 167);
        _gamesvc.getSeries(_coll.owner.userId, card.categoryId, new PopupCallback<Series>() {
            public void onSuccess (Series series) {
                final SeriesPanel panel = new SeriesPanel(_ctx, _coll.owner.userId, series, owned);
                if (_clearSeries != null) {
                    _clearSeries.execute();
                }
                final SimplePanel wrapper = Widgets.newSimplePanel(null, panel);
                add(wrapper);
                FX.reveal(wrapper).fromTop().run(animTime);
                _clearSeries = new Command() {
                    public void execute () {
                        FX.unreveal(wrapper).fromBottom().onComplete(new Command() {
                            public void execute () {
                                remove(wrapper);
                            }
                        }).run(animTime);
                    }
                };
            }
        });
    }

    protected static Map<String, String> toCodons (Iterable<String> catnames)
    {
        Map<String, String> map = new HashMap<String, String>();
        Set<String> seen = new HashSet<String>();
        for (String catname : catnames) {
            String codon = catname.substring(0, 2);
            // if we already have (0, 1) then fall back to (0, 2)
            if (seen.contains(codon)) {
                codon = catname.substring(0, 1) + catname.substring(2, 3);
            }
            map.put(catname, codon);
            seen.add(codon);
        }
        return map;
    }

    protected static Image createLine (boolean up, boolean down, boolean left, boolean right)
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
    protected String _selcat;
    protected PlayerCollection _coll;
    protected FluentTable _header;
    protected Widget _taxon;
    protected Value<Integer> _cards, _series, _completed;
    protected Command _clearSeries;

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
