//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.client.editor;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.core.client.GWT;

import com.threerings.everything.rpc.EditorService;
import com.threerings.everything.rpc.EditorServiceAsync;
import com.threerings.everything.data.Category;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.ui.DataPanel;
import com.threerings.everything.client.util.Args;
import com.threerings.everything.client.util.Context;
import com.threerings.everything.client.util.Page;

/**
 * Displays the series created by the viewer.
 */
public class MySeriesPanel extends DataPanel<List<Category>>
{
    public MySeriesPanel (Context ctx)
    {
        super(ctx, "handwriting");
        _editorsvc.loadMySeries(createCallback());
    }

    protected void init (List<Category> data)
    {
        Collections.sort(data, MY_SORT);
        // sort the series with unshipped at the top and shipped at the bottom
        for (Category cat : data) {
            String prefix;
            switch (cat.state) {
            default:
            case IN_DEVELOPMENT: prefix = "\u25CB"; break;
            case PENDING_REVIEW: prefix = "\u25D1"; break;
            case ACTIVE: prefix = "\u25CF"; break;
            }
            add(Widgets.newFlowPanel(
                    Widgets.newInlineLabel(prefix + " "),
                    Args.createInlink(cat.name, Page.EDIT_SERIES, cat.categoryId)));
        }
    }

    protected static final Comparator<Category> MY_SORT = new Comparator<Category>() {
        public int compare (Category c1, Category c2) {
            return (c1.state == c2.state) ?
                c1.name.compareTo(c2.name) : c1.state.compareTo(c2.state);
        }
    };

    protected static final EditorServiceAsync _editorsvc = GWT.create(EditorService.class);
}
