//
// $Id$

package client.admin;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.DefaultTextListener;
import com.threerings.gwt.ui.EnumListBox;
import com.threerings.gwt.ui.LimitedTextArea;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.AdminService;
import com.threerings.everything.client.AdminServiceAsync;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.Thing;

import client.game.CardView;
import client.util.ClickCallback;
import client.util.Context;
import client.util.PopupCallback;

/**
 * Provides an interface for editing categories and things.
 */
public class EditThingsPanel extends SmartTable
{
    public EditThingsPanel (Context ctx)
    {
        super("editThings", 5, 0);

        setWidget(0, 0, _cats);
        setWidget(0, 1, _subcats);
        setWidget(0, 2, _series);
        setWidget(0, 3, _things);
        for (int col = 0; col < getCellCount(0); col++) {
            getFlexCellFormatter().setVerticalAlignment(0, col, HasAlignment.ALIGN_TOP);
        }

        _ctx = ctx;
        _cats.setChild(_subcats);
        _subcats.setChild(_series);
        _series.setChild(_things);
        _cats.load();
    }

    protected abstract class Column<T> extends FlowPanel
    {
        public Column (String header, int maxlen) {
            setStyleName("Column");
            add(Widgets.newLabel(header, "Header"));
            add(_input = Widgets.newTextBox("", maxlen, 15));
            add(_contents = Widgets.newFlowPanel("List"));
            add(_empty = Widgets.newLabel("<empty>", null));
            DefaultTextListener.configure(_input, "<add new>");

            // wire up our create callback
            new ClickCallback<Integer>(new Button("dummy"), _input) {
                protected boolean callService () {
                    String text = _input.getText().trim();
                    if (text.length() == 0) {
                        return false;
                    }
                    _item = onCreate(text, this);
                    return true;
                }
                protected boolean gotResult (Integer result) {
                    onCreated(result, _item);
                    _item = null;
                    _input.setText("");
                    return true;
                }
                protected T _item;
            };

            // start off disabled
            setEnabled(false);
        }

        public void setChild (Column<?> child) {
            _child = child;
        }

        public void load () {
            clear();
            _contents.add(Widgets.newLabel("Loading...", null));
            callLoad(new PopupCallback<List<T>>(_input) {
                public void onSuccess (List<T> items) {
                    _contents.clear();
                    for (T item : items) {
                        addItem(item);
                    }
                    _empty.setVisible(items.size() == 0);
                    setEnabled(true);
                }
            });
        }

        public void clear () {
            setEnabled(false);
            _contents.clear();
            if (_child != null) {
                _child.clear();
            }
        }

        public void setEnabled (boolean enabled) {
            _input.setEnabled(enabled);
        }

        public T getSelected () {
            return _selected;
        }

        public void setSelected (T item) {
            _selected = item;
            // TODO: restyle our labels
            if (_child != null) {
                if (_selected != null) {
                    _child.load();
                } else {
                    _child.clear();
                }
            }
        }

        public void addItem (final T item) {
            final HorizontalPanel row = new HorizontalPanel();
            final Label label = Widgets.newActionLabel(getName(item), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    setSelected(item);
                }
            });
            row.add(Widgets.newActionImage("images/delete.png", "Delete", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    onDelete(item, new PopupCallback<Void>(label) {
                        public void onSuccess (Void result) {
                            _contents.remove(row);
                            if (item == _selected) {
                                setSelected(null);
                            }
                        }
                    });
                }
            }));
            row.add(Widgets.newShim(5, 5));
            row.add(label);
            _contents.add(row);
        }

        protected void itemAdded (T item) {
            _empty.setVisible(false);
            addItem(item);
            setSelected(item);
        }

        protected abstract void callLoad (AsyncCallback<List<T>> callback);
        protected abstract String getName (T object);
        protected abstract T onCreate (String text, AsyncCallback<Integer> callback);
        protected abstract void onCreated (int createdId, T object);
        protected abstract void onDelete (T object, AsyncCallback<Void> callback);

        protected T _selected;
        protected Column<?> _child;
        protected FlowPanel _contents;
        protected TextBox _input;
        protected Label _empty;
    }

    protected abstract class CategoryColumn extends Column<Category>
    {
        public CategoryColumn (String title) {
            super(title, Category.MAX_NAME_LENGTH);
        }

        protected String getName (Category category) {
            return category.name;
        }

        protected Category onCreate (String text, AsyncCallback<Integer> callback) {
            Category cat = new Category();
            cat.name = text;
            cat.parentId = _parentId;
            _adminsvc.createCategory(cat, callback);
            return cat;
        }

        protected void onCreated (int createdId, Category cat) {
            cat.categoryId = createdId;
            itemAdded(cat);
        }

        protected void onDelete (Category category, AsyncCallback<Void> callback) {
            _adminsvc.deleteCategory(category.categoryId, callback);
        }

        protected int _parentId;
    }

    protected class ThingEditor extends FlowPanel
    {
        public ThingEditor (final Card card) {
            setStyleName("Editor");
            SmartTable bits = new SmartTable(5, 0);
            add(bits);

            int row = 0;
            bits.setText(row, 0, "Name", 1, "Label");
            final TextBox name = Widgets.newTextBox(card.thing.name, Thing.MAX_NAME_LENGTH, 15);
            bits.setWidget(row, 1, name);

            bits.setText(row, 2, "Rarity", 1, "Label");
            final EnumListBox<Rarity> rarity = new EnumListBox<Rarity>(Rarity.class);
            rarity.setSelectedValue(card.thing.rarity);
            bits.setWidget(row++, 3, rarity);
            rarity.addChangeHandler(new ChangeHandler() {
                public void onChange (ChangeEvent event) {
                    card.thing.rarity = rarity.getSelectedValue();
                    updateCard(card);
                }
            });

            bits.setText(row, 0, "Descrip", 1, "Label");
            final LimitedTextArea descrip = Widgets.newTextArea(
                card.thing.descrip, -1, 2, Thing.MAX_DESCRIP_LENGTH);
            bits.setWidget(row++, 1, descrip, 3, "Wide");

            bits.setText(row, 0, "Facts", 1, "Label");
            final LimitedTextArea facts = Widgets.newTextArea(
                card.thing.facts, -1, 4, Thing.MAX_FACTS_LENGTH);
            bits.setWidget(row++, 1, facts, 3, "Wide");

            bits.setText(row, 0, "Source", 1, "Label");
            final TextBox source = Widgets.newTextBox(card.thing.descrip, 255, -1);
            bits.setWidget(row++, 1, source, 3, "Wide");

            final Button save = new Button("Save");
            bits.getFlexCellFormatter().setHorizontalAlignment(row, 1, HasAlignment.ALIGN_RIGHT);
            bits.setWidget(row++, 1, save, 3, null);
            new ClickCallback<Void>(save) {
                protected boolean callService () {
                    _adminsvc.updateThing(card.thing, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    Popups.infoNear("Thing saved.", save);
                    return true;
                }
            };

            final Timer updater = new Timer() {
                @Override public void run () {
                    card.thing.name = name.getText().trim();
                    card.thing.descrip = descrip.getText().trim();
                    card.thing.facts = facts.getText().trim();
                    card.thing.source = source.getText().trim();
                    updateCard(card);
                }
            };

            KeyPressHandler trigger = new KeyPressHandler() {
                public void onKeyPress (KeyPressEvent event) {
                    updater.cancel();
                    updater.schedule(250);
                }
            };
            name.addKeyPressHandler(trigger);
            descrip.getTextArea().addKeyPressHandler(trigger);
            facts.getTextArea().addKeyPressHandler(trigger);
            source.addKeyPressHandler(trigger);

            updateCard(card);
        }

        protected void updateCard (Card card) {
            while (getWidgetCount() > 1) {
                remove(1);
            }
            SmartTable pair = new SmartTable(5, 0);
            pair.setWidget(0, 0, new CardView.Front(card));
            pair.setWidget(0, 1, new CardView.Back(card));
            add(pair);
        }
    }

    protected CategoryColumn _cats = new CategoryColumn("Categories") {
        protected void callLoad (AsyncCallback<List<Category>> callback) {
            _adminsvc.loadCategories(0, callback);
        }
    };

    protected CategoryColumn _subcats = new CategoryColumn("Sub-categories") {
        protected void callLoad (AsyncCallback<List<Category>> callback) {
            Category parent = _cats.getSelected();
            if (parent != null) {
                _adminsvc.loadCategories(_parentId = parent.categoryId, callback);
            }
        }
    };

    protected CategoryColumn _series = new CategoryColumn("Series") {
        protected void callLoad (AsyncCallback<List<Category>> callback) {
            Category parent = _subcats.getSelected();
            if (parent != null) {
                _adminsvc.loadCategories(_parentId = parent.categoryId, callback);
            }
        }
    };

    protected Column<Thing> _things = new Column<Thing>("Things", Thing.MAX_NAME_LENGTH) {
        protected void callLoad (AsyncCallback<List<Thing>> callback) {
            Category category = _series.getSelected();
            if (category != null) {
                _adminsvc.loadThings(_categoryId = category.categoryId, callback);
            }
        }

        protected String getName (Thing thing) {
            return thing.name;
        }

        protected Thing onCreate (String text, AsyncCallback<Integer> callback) {
            Thing thing = new Thing();
            thing.name = text;
            thing.categoryId = _categoryId;
            // set up the thing with defaults which can be edited once it is created
            thing.rarity = Rarity.I;
            thing.image = "";
            thing.descrip = "TODO";
            thing.facts = "TODO";
            thing.source = "TODO";
            _adminsvc.createThing(thing, callback);
            return thing;
        }

        protected void onCreated (int createdId, Thing thing) {
            thing.thingId = createdId;
            itemAdded(thing);
        }

        protected void onDelete (Thing object, AsyncCallback<Void> callback) {
            _adminsvc.deleteThing(object.thingId, callback);
        }

        @Override public void clear () {
            super.clear();
            setText(1, 0, "", getCellCount(0), null);
        }

        @Override public void setSelected (Thing item) {
            super.setSelected(item);
            // create a fake card and display it
            Card card = new Card();
            card.owner = _ctx.getMe();
            card.categories = new Category[] {
                _cats.getSelected(), _subcats.getSelected(), _series.getSelected() };
            card.thing = item;
            card.created = new Date();
            setWidget(1, 0, new ThingEditor(card), getCellCount(0), null);
        }

        protected int _categoryId;
    };

    protected Context _ctx;

    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);
}
