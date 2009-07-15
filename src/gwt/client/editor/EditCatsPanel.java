//
// $Id$

package client.editor;

import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
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
            _empty.setVisible(false);
            _contents.add(Widgets.newLabel("Loading...", null));
            _parentId = parentId;
            _ctx.getCatsModel().getCategories(_parentId, new PopupCallback<List<Category>>(_input) {
                public void onSuccess (List<Category> cats) {
                    _contents.clear();
                    for (Category cat : cats) {
                        addCat(cat);
                    }
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
            final Widget label = selected ? Widgets.newLabel(row.cat.name, "Selected") :
                createCatActionLabel(row.cat);
            label.setTitle(row.cat.creator.toString());
            row.add(Widgets.newActionImage("images/delete.png", "Delete", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _ctx.getCatsModel().deleteCategory(
                        row.cat.categoryId, new PopupCallback<Void>(label) {
                        public void onSuccess (Void result) {
                            _contents.remove(row);
                            if (row.cat.categoryId == _selectedId) {
                                setSelectedId(0);
                            }
                        }
                    });
                }
            }));
            row.add(Widgets.newShim(5, 5));
            row.add(label);
            return row;
        }

        protected abstract Widget createCatActionLabel (final Category cat);

        protected Column _child;
        protected FlowPanel _contents;
        protected TextBox _input;
        protected Label _empty;

        protected int _parentId = -1, _selectedId, _pendingSelId = -1;
    }

    protected Column _cats = new Column("Categories") {
        protected Widget createCatActionLabel (Category cat) {
            return Args.createLink(cat.name, Page.EDIT_CATS, cat.categoryId);
        }
    };
    protected Column _subcats = new Column("Sub-categories") {
        protected Widget createCatActionLabel (Category cat) {
            return Args.createLink(cat.name, Page.EDIT_CATS, _cats.getSelectedId(), cat.categoryId);
        }
    };
    protected Column _series = new Column("Series") {
        protected Widget createCatActionLabel (Category cat) {
            return Widgets.newFlowPanel(
                Args.createInlink(cat.name, Page.EDIT_SERIES, cat.categoryId),
                Widgets.newLabel(" (" + cat.things + ")", "inline"));
        }
    };

    protected Context _ctx;
}
