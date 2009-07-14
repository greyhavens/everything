//
// $Id$

package client.editor;

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
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.DefaultTextListener;
import com.threerings.gwt.ui.EnumListBox;
import com.threerings.gwt.ui.LimitedTextArea;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.ClickCallback;

import com.threerings.everything.client.EditorService;
import com.threerings.everything.client.EditorServiceAsync;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.Thing;

import client.game.CardView;
import client.util.Args;
import client.util.Context;
import client.util.MediaUploader;
import client.util.Page;
import client.util.PopupCallback;

/**
 * Provides an interface for editing categories and things.
 */
public class EditCatsPanel extends SmartTable
{
    public EditCatsPanel (Context ctx)
    {
        super("editCats", 5, 0);

        setWidget(0, 0, _cats);
        setWidget(0, 1, _subcats);
        setWidget(0, 2, _series);
        for (int col = 0; col < getCellCount(0); col++) {
            getFlexCellFormatter().setVerticalAlignment(0, col, HasAlignment.ALIGN_TOP);
        }

        _ctx = ctx;
        _cats.setChild(_subcats);
        _subcats.setChild(_series);
        _cats.load();
    }

    protected static class Row<T> extends HorizontalPanel
    {
        public final T item;
        public Row (T item) {
            this.item = item;
        }
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
                    setEnabled(isEditable());
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
            // deselect the old item row and select the new
            for (int ii = 0; ii < _contents.getWidgetCount(); ii++) {
                Row<T> row = (Row<T>)_contents.getWidget(ii);
                if (row.item == _selected) {
                    initItemRow(row, false);
                } else if (row.item == item) {
                    initItemRow(row, true);
                }
            }
            _selected = item;
            if (_child != null) {
                if (_selected != null) {
                    _child.load();
                } else {
                    _child.clear();
                }
            }
        }

        public void addItem (T item) {
            Row<T> row = new Row<T>(item);
            initItemRow(row, false);
            _contents.add(row);
        }

        protected boolean isEditable () {
            return true;
        }

        protected void initItemRow (final Row<T> row, boolean selected)
        {
            row.clear();
            final Widget label = selected ? Widgets.newLabel(getName(row.item), "Selected") :
                createItemActionLabel(row.item);
            row.add(Widgets.newActionImage("images/delete.png", "Delete", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    onDelete(row.item, new PopupCallback<Void>(label) {
                        public void onSuccess (Void result) {
                            _contents.remove(row);
                            if (row.item == _selected) {
                                setSelected(null);
                            }
                        }
                    });
                }
            }));
            row.add(Widgets.newShim(5, 5));
            row.add(label);
        }

        protected Widget createItemActionLabel (final T item)
        {
            return Widgets.newActionLabel(getName(item), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    setSelected(item);
                }
            });
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
            _editorsvc.createCategory(cat, callback);
            return cat;
        }

        protected void onCreated (int createdId, Category cat) {
            cat.categoryId = createdId;
            itemAdded(cat);
        }

        protected void onDelete (Category category, AsyncCallback<Void> callback) {
            _editorsvc.deleteCategory(category.categoryId, callback);
        }

        protected int _parentId;
    }

    protected CategoryColumn _cats = new CategoryColumn("Categories") {
        protected void callLoad (AsyncCallback<List<Category>> callback) {
            _editorsvc.loadCategories(0, callback);
        }
    };

    protected CategoryColumn _subcats = new CategoryColumn("Sub-categories") {
        protected void callLoad (AsyncCallback<List<Category>> callback) {
            Category parent = _cats.getSelected();
            if (parent != null) {
                _editorsvc.loadCategories(_parentId = parent.categoryId, callback);
            }
        }
    };

    protected CategoryColumn _series = new CategoryColumn("Series") {
        protected void callLoad (AsyncCallback<List<Category>> callback) {
            Category parent = _subcats.getSelected();
            if (parent != null) {
                _editorsvc.loadCategories(_parentId = parent.categoryId, callback);
            }
        }

        @Override protected Widget createItemActionLabel (Category item) {
            return Args.createLink(getName(item), Page.EDIT_SERIES, item.categoryId);
        }
    };

//         @Override protected boolean isEditable () {
//             Category series = _series.getSelected();
//             return !series.active && (series.creatorId == _ctx.getMe().userId || _ctx.isAdmin());
//         }

//         @Override protected void initItemRow (Row<Thing> row, boolean selected) {
//             super.initItemRow(row, selected);
//             row.add(Widgets.newShim(5, 5));
//             row.add(Widgets.newLabel(row.item.rarity.toString(), selected ? "Selected" : null));
//         }

//         protected Thing onCreate (String text, AsyncCallback<Integer> callback) {
//             Thing thing = new Thing();
//             thing.name = text;
//             thing.categoryId = _categoryId;
//             // set up the thing with defaults which can be edited once it is created
//             thing.rarity = Rarity.I;
//             thing.image = "";
//             thing.descrip = "Briefly describe your thing here. Scroll down to make sure " +
//                 "your description and facts fit on the Thing display.";
//             thing.facts = "Enter facts about your thing here.\nPress enter to end one " +
//                 "bullet point and start the next.";
//             thing.source = "http://wikipedia.org/Todo";
//             _editorsvc.createThing(thing, callback);
//             return thing;
//         }

//         protected void onCreated (int createdId, Thing thing) {
//             thing.thingId = createdId;
//             itemAdded(thing);
//         }

//         protected void onDelete (Thing object, AsyncCallback<Void> callback) {
//             _editorsvc.deleteThing(object.thingId, callback);
//         }

//         protected int _categoryId;
//     };

    protected Context _ctx;

    protected static final EditorServiceAsync _editorsvc = GWT.create(EditorService.class);
}
