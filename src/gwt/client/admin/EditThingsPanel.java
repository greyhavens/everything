//
// $Id$

package client.admin;

import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.ClickCallback;

import com.threerings.everything.client.AdminService;
import com.threerings.everything.client.AdminServiceAsync;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.Thing;
import com.threerings.everything.data.ThingSet;

import client.util.Context;
import client.util.PopupCallback;

/**
 * Provides an interface for editing categories, sets and things.
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

        _cats.setChild(_subcats);
        _subcats.setChild(_series);
        _series.setChild(_things);
        _cats.load();
    }

    protected abstract class Column<T> extends FlowPanel
    {
        public Column (String header, int maxlen) {
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
                protected void reportFailure (Throwable cause) {
                    Popups.errorNear(cause.getMessage(), _input);
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

    protected Column<Category> _cats =
        new Column<Category>("Categories", Category.MAX_NAME_LENGTH) {
        protected void callLoad (AsyncCallback<List<Category>> callback) {
            _adminsvc.loadCategories(0, callback);
        }

        protected String getName (Category category) {
            return category.name;
        }

        protected Category onCreate (String text, AsyncCallback<Integer> callback) {
            Category cat = new Category();
            cat.name = text;
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
    };

    protected Column<Category> _subcats =
        new Column<Category>("Sub-categories", Category.MAX_NAME_LENGTH) {
        protected void callLoad (AsyncCallback<List<Category>> callback) {
            Category parent = _cats.getSelected();
            if (parent != null) {
                _adminsvc.loadCategories(_parentId = parent.categoryId, callback);
            }
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

        protected void onCreated (int createdId, Category category) {
            category.categoryId = createdId;
            itemAdded(category);
        }

        protected void onDelete (Category category, AsyncCallback<Void> callback) {
            _adminsvc.deleteCategory(category.categoryId, callback);
        }

        protected int _parentId;
    };

    protected Column<ThingSet> _series = new Column<ThingSet>("Series", ThingSet.MAX_NAME_LENGTH) {
        protected void callLoad (AsyncCallback<List<ThingSet>> callback) {
            Category category = _subcats.getSelected();
            if (category != null) {
                _adminsvc.loadSets(_categoryId = category.categoryId, callback);
            }
        }

        protected String getName (ThingSet set) {
            return set.name;
        }

        protected ThingSet onCreate (String text, AsyncCallback<Integer> callback) {
            ThingSet set = new ThingSet();
            set.name = text;
            set.categoryId = _categoryId;
            _adminsvc.createSet(set, callback);
            return set;
        }

        protected void onCreated (int createdId, ThingSet set) {
            set.setId = createdId;
            itemAdded(set);
        }

        protected void onDelete (ThingSet set, AsyncCallback<Void> callback) {
            _adminsvc.deleteSet(set.setId, callback);
        }

        protected int _categoryId;
    };

    protected Column<Thing> _things = new Column<Thing>("Things", Thing.MAX_NAME_LENGTH) {
        protected void callLoad (AsyncCallback<List<Thing>> callback) {
            ThingSet set = _series.getSelected();
            if (set != null) {
                _adminsvc.loadThings(_setId = set.setId, callback);
            }
        }

        protected String getName (Thing thing) {
            return thing.name;
        }

        protected Thing onCreate (String text, AsyncCallback<Integer> callback) {
            Thing thing = new Thing();
            thing.name = text;
            thing.setId = _setId;
            // set up the thing with defaults which can be edited once it is created
            thing.rarity = Rarity.I;
            thing.image = "";
            thing.descrip = "TODO";
            thing.facts = "TODO";
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

        protected int _setId;
    };

    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);
}
