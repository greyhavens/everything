//
// $Id$

package client.editor;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.DefaultTextListener;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.EditorService;
import com.threerings.everything.client.EditorServiceAsync;
import com.threerings.everything.data.Category;

import client.util.Args;
import client.util.ClickCallback;
import client.util.Context;
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

    protected static class Row extends HorizontalPanel
    {
        public final Category cat;
        public Row (Category cat) {
            this.cat = cat;
        }
    }

    protected abstract class Column extends FlowPanel
    {
        public Column (String header) {
            setStyleName("Column");
            add(Widgets.newLabel(header, "Header"));
            add(_input = Widgets.newTextBox("", Category.MAX_NAME_LENGTH, 15));
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
                    _cat = new Category();
                    _cat.name = text;
                    _cat.parentId = _parentId;
                    _cat.creator = _ctx.getMe();
                    _editorsvc.createCategory(_cat, this);
                    return true;
                }
                protected boolean gotResult (Integer categoryId) {
                    _cat.categoryId = categoryId;
                    _empty.setVisible(false);
                    addCat(_cat);
                    if (_child != null) {
                        setSelected(_cat);
                    }
                    _cat = null;
                    _input.setText("");
                    return true;
                }
                protected Category _cat;
            };

            // start off disabled
            setEnabled(false);
        }

        public void setChild (Column child) {
            _child = child;
        }

        public void load () {
            clear();
            _contents.add(Widgets.newLabel("Loading...", null));
            callLoad(new PopupCallback<List<Category>>(_input) {
                public void onSuccess (List<Category> cats) {
                    _contents.clear();
                    for (Category cat : cats) {
                        addCat(cat);
                    }
                    _empty.setVisible(cats.size() == 0);
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

        public Category getSelected () {
            return _selected;
        }

        public void setSelected (Category cat) {
            // deselect the old cat row and select the new
            for (int ii = 0; ii < _contents.getWidgetCount(); ii++) {
                Row row = (Row)_contents.getWidget(ii);
                if (row.cat == _selected) {
                    initCatRow(row, false);
                } else if (row.cat == cat) {
                    initCatRow(row, true);
                }
            }
            _selected = cat;
            if (_child != null) {
                if (_selected != null) {
                    _child.load();
                } else {
                    _child.clear();
                }
            }
        }

        public void addCat (Category cat) {
            Row row = new Row(cat);
            initCatRow(row, false);
            _contents.add(row);
        }

        protected void initCatRow (final Row row, boolean selected)
        {
            row.clear();
            final Widget label = selected ? Widgets.newLabel(row.cat.name, "Selected") :
                createCatActionLabel(row.cat);
            label.setTitle(row.cat.creator.toString());
            row.add(Widgets.newActionImage("images/delete.png", "Delete", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _editorsvc.deleteCategory(row.cat.categoryId, new PopupCallback<Void>(label) {
                        public void onSuccess (Void result) {
                            _contents.remove(row);
                            if (row.cat == _selected) {
                                setSelected(null);
                            }
                        }
                    });
                }
            }));
            row.add(Widgets.newShim(5, 5));
            row.add(label);
        }

        protected Widget createCatActionLabel (final Category cat)
        {
            return Widgets.newActionLabel(cat.name, new ClickHandler() {
                public void onClick (ClickEvent event) {
                    setSelected(cat);
                }
            });
        }

        protected abstract void callLoad (AsyncCallback<List<Category>> callback);

        protected Category _selected;
        protected Column _child;
        protected FlowPanel _contents;
        protected TextBox _input;
        protected Label _empty;
        protected int _parentId;
    }

    protected Column _cats = new Column("Categories") {
        protected void callLoad (AsyncCallback<List<Category>> callback) {
            _editorsvc.loadCategories(0, callback);
        }
    };

    protected Column _subcats = new Column("Sub-categories") {
        protected void callLoad (AsyncCallback<List<Category>> callback) {
            Category parent = _cats.getSelected();
            if (parent != null) {
                _editorsvc.loadCategories(_parentId = parent.categoryId, callback);
            }
        }
    };

    protected Column _series = new Column("Series") {
        protected void callLoad (AsyncCallback<List<Category>> callback) {
            Category parent = _subcats.getSelected();
            if (parent != null) {
                _editorsvc.loadCategories(_parentId = parent.categoryId, callback);
            }
        }

        @Override protected Widget createCatActionLabel (Category cat) {
            return Widgets.newFlowPanel(
                Args.createInlink(cat.name, Page.EDIT_SERIES, cat.categoryId),
                Widgets.newLabel(" (" + cat.things + ")", "inline"));
        }
    };

    protected Context _ctx;

    protected static final EditorServiceAsync _editorsvc = GWT.create(EditorService.class);
}
