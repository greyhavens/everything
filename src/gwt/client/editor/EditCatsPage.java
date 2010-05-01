//
// $Id$

package client.editor;

import java.util.List;

import com.google.common.base.Function;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.data.Category;

import client.util.Args;
import client.util.ClickCallback;
import client.util.Context;
import client.util.Errors;
import client.util.Page;
import client.util.PopupCallback;

/**
 * Provides an interface for editing categories and things.
 */
public class EditCatsPage extends FluentTable
{
    public EditCatsPage (Context ctx)
    {
        super(5, 0, "page", "editCats");

        FlowPanel faq = Widgets.newFlowPanel(
            Widgets.newLabel("New editors, read:", "machine"),
            Args.createLink("Editor Guidelines", "handwriting", Page.EDIT_FAQ),
            Widgets.newShim(10, 10),
            Widgets.newLabel("Your Series", "machine"), new MySeriesPanel(ctx));

        at(0, 0).setWidget(_cats).alignTop();
        at(0, 1).setWidget(_subcats).alignTop();
        at(0, 2).setWidget(_series).alignTop();
        at(0, 3).setWidget(Widgets.newShim(25, 10)).alignTop();
        at(0, 4).setWidget(faq).alignTop();
        at(1, 0).setHTML(_msgs.ecTip(), "handwriting").setColSpan(5);

        _ctx = ctx;
        _cats.setChild(_subcats);
        _subcats.setChild(_series);
    }

    public void setArgs (Args args)
    {
        _cats.setSelectedId(args.get(0, -1));
        _subcats.setSelectedId(args.get(1, -1));
        _cats.setParentId(0);
    }

    protected String createToken ()
    {
        return Args.createLinkToken(
            Page.EDIT_CATS, _cats.getSelectedId(), _subcats.getSelectedId());
    }

    protected static class Row extends FlowPanel
    {
        public final Category cat;
        public Row (Category cat) {
            this.cat = cat;
        }
    }

    protected abstract class Column extends FlowPanel
    {
        public Column (String header, int level) {
            setStyleName("Column");
            addStyleName("handwriting");
            add(Widgets.newLabel(header, "Header", "machine"));
            _level = level;
            add(_input = Widgets.newTextBox("", Category.MAX_NAME_LENGTH, 15));
            add(_contents = Widgets.newFlowPanel("List"));
            add(_empty = Widgets.newLabel("<empty>"));
            Widgets.setPlaceholderText(_input, "<add new>");

            // wire up our create callback
            new ClickCallback<Category>(new Button("dummy"), _input) {
                protected boolean callService () {
                    String text = _input.getText().trim();
                    if (text.length() == 0) {
                        return false;
                    }
                    _ctx.getCatsModel().createCategory(text, _parentId, this);
                    return true;
                }
                protected boolean gotResult (Category cat) {
                    _empty.setVisible(false);
                    addCat(cat);
                    _input.setText("");
                    if (_child != null) {
                        setSelectedId(cat.categoryId);
                        History.newItem(createToken());
                    }
                    return true;
                }
            };

            // start off disabled
            setEnabled(false);
        }

        public int getSelectedId () {
            return _selectedId;
        }

        public void setSelectedId (int categoryId)
        {
            if (_contents.getWidgetCount() == 0) {
                _pendingSelId = categoryId;

            } else if (_selectedId != categoryId) {
                // deselect the old cat row and select the new
                for (int ii = 0; ii < _contents.getWidgetCount(); ii++) {
                    Row row = (Row)_contents.getWidget(ii);
                    if (row.cat.categoryId == _selectedId) {
                        initCatRow(row, false);
                    } else if (row.cat.categoryId == categoryId) {
                        initCatRow(row, true);
                    }
                }
                _selectedId = categoryId;
                if (_child != null) {
                    if (_selectedId > 0) {
                        _child.setParentId(_selectedId);
                    } else {
                        _child.clear();
                    }
                }
            }
        }

        public void setParentId (int parentId)
        {
            if (_parentId == parentId) {
                checkPendingSelection();
                return;
            }

            clear();
            _empty.setText("Loading...");
            _empty.setVisible(true);
            _parentId = parentId;
            _ctx.getCatsModel().getCategories(_parentId, new PopupCallback<List<Category>>(_input) {
                public void onSuccess (List<Category> cats) {
                    _contents.clear();
                    for (Category cat : cats) {
                        if (shouldDisplayCategory(cat)) {
                            addCat(cat);
                        }
                    }
                    _empty.setText("<empty>");
                    _empty.setVisible(cats.size() == 0);
                    checkPendingSelection();
                    setEnabled(true);
                }
            });
        }

        public void setChild (Column child) {
            _child = child;
        }

        public void clear () {
            setEnabled(false);
            _contents.clear();
            if (_child != null) {
                _child.clear();
            }
        }

        protected void setEnabled (boolean enabled) {
            _input.setEnabled(enabled);
        }

        protected void addCat (Category cat) {
            _contents.add(initCatRow(new Row(cat), false));
        }

        protected void checkPendingSelection () {
            if (_pendingSelId > 0) {
                setSelectedId(_pendingSelId);
                _pendingSelId = 0;
            }
        }

        protected Row initCatRow (final Row row, boolean selected)
        {
            row.clear();
            if (_ctx.isAdmin() || _ctx.getMe().equals(row.cat.creator)) {
                row.add(Widgets.newActionImage("images/folder.png", new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        showOptionMenu(row, (Widget)event.getSource());
                    }
                }));
                row.add(Widgets.newInlineLabel(" "));
            }
            if (selected) {
                row.add(Widgets.newInlineLabel(row.cat.name, "Selected"));
            } else {
                addCategoryAction(row);
            }
            row.setTitle(""+row.cat.creator);
            return row;
        }

        protected void removeRow (Row row)
        {
            _contents.remove(row);
            if (row.cat.categoryId == _selectedId) {
                setSelectedId(0);
            }
        }

        protected void showOptionMenu (final Row row, final Widget trigger)
        {
            final PopupPanel popup = new PopupPanel(true);
            MenuBar options = new MenuBar(true);
            options.addItem("Delete", new Command() {
                public void execute() {
                    popup.hide();
                    _ctx.getCatsModel().deleteCategory(
                        row.cat.categoryId, new PopupCallback<Void>(trigger) {
                        public void onSuccess (Void result) {
                            removeRow(row);
                        }
                    });
                }
            });
            if (_level > 0) {
                Function<Category, Void> onSelect = new Function<Category, Void>() {
                    public Void apply (final Category parent) {
                        popup.hide();
                        _ctx.getCatsModel().moveCategory(
                            row.cat, parent.categoryId, new PopupCallback<Void>() {
                            public void onSuccess (Void result) {
                                removeRow(row);
                                Popups.info(row.cat.name + " moved to " + parent.name + ".");
                            }
                        });
                        return null;
                    }
                };
                options.addItem("Move", new CategoryMenuBar(0, _level-1, onSelect));
            }
            popup.setWidget(options);
            Popups.showNear(popup, trigger);
        }

        protected boolean shouldDisplayCategory (Category cat)
        {
            return true;
        }

        protected abstract void addCategoryAction (Row row);

        protected int _level;
        protected Column _child;
        protected FlowPanel _contents;
        protected TextBox _input;
        protected Label _empty;

        protected int _parentId = -1, _selectedId, _pendingSelId = -1;
    }

    protected class CategoryMenuBar extends MenuBar
        implements AsyncCallback<List<Category>>
    {
        public CategoryMenuBar (int parentId, int children, Function<Category, Void> onSelect) {
            super(true);
            _parentId = parentId;
            _children = children;
            _onSelect = onSelect;
        }

        public void onAttach () {
            super.onAttach();
            if (!_resolved) {
                _resolved = true;
                _ctx.getCatsModel().getCategories(_parentId, this);
            }
        }

        // from interface AsyncCallback
        public void onSuccess (List<Category> cats) {
            for (final Category cat : cats) {
                if (_children > 0) {
                    addItem(cat.name, new CategoryMenuBar(cat.categoryId, _children-1, _onSelect));
                } else {
                    addItem(cat.name, new Command() {
                        public void execute () {
                            _onSelect.apply(cat);
                        }
                    });
                }
            }
            if (cats.size() == 0) {
                addItem("<empty>", new Command() {
                    public void execute () { /* noop! */ }
                });
            }
        }

        // from interface AsyncCallback
        public void onFailure (Throwable cause) {
            addItem("Error: " + Errors.xlate(cause), new Command() {
                public void execute () { /* noop! */ }
            });
        }

        protected int _parentId, _children;
        protected Function<Category, Void> _onSelect;
        protected boolean _resolved;
    }

    protected Column _cats = new Column("Category", 0) {
        protected void addCategoryAction (Row row) {
            row.add(Args.createInlink(row.cat.name, Page.EDIT_CATS, row.cat.categoryId));
        }
    };
    protected Column _subcats = new Column("Sub-category", 1) {
        protected void addCategoryAction (Row row) {
            row.add(Args.createInlink(row.cat.name, Page.EDIT_CATS, _cats.getSelectedId(),
                                      row.cat.categoryId));
        }
    };
    protected Column _series = new Column("Series", 2) {
        protected void addCategoryAction (Row row) {
            row.add(Args.createInlink(row.cat.name, Page.EDIT_SERIES, row.cat.categoryId));
            row.add(Widgets.newInlineLabel(" (" + row.cat.things + ")"));
        }
        protected boolean shouldDisplayCategory (Category cat) {
            return _ctx.isAdmin() || _ctx.getMe().equals(cat.creator) || (cat.things > 0);
        }
    };

    protected Context _ctx;

    protected static final EditorMessages _msgs = GWT.create(EditorMessages.class);
}
