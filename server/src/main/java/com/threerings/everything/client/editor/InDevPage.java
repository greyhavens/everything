//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;

import com.threerings.gwt.ui.FluentTable;

import com.threerings.everything.data.Category;
import com.threerings.everything.rpc.EditorService;
import com.threerings.everything.rpc.EditorServiceAsync;

import com.threerings.everything.client.ui.DataPanel;
import com.threerings.everything.client.util.Args;
import com.threerings.everything.client.util.Context;
import com.threerings.everything.client.util.Page;
import com.threerings.everything.client.util.PopupCallback;

/**
 * Displays all of the in development series.
 */
public class InDevPage extends DataPanel<EditorService.InDevResult>
{
    public InDevPage (Context ctx)
    {
        super(ctx, "page", "indev");
    }

    public void setMode (String mode)
    {
        try {
            _mode = Mode.valueOf(mode);
        } catch (Exception e) {
            _mode = Mode.NAME;
        }
        if (_infos == null) {
            _editorsvc.loadInDevSeries(createCallback());
        } else {
            init(_infos);
        }
    }

    protected void init (EditorService.InDevResult data)
    {
        // map our parent categories by id
        Map<Integer,Category> parents = new HashMap<Integer,Category>();
        for (Category cat : data.parents) parents.put(cat.categoryId, cat);
        // now resolve the paths to our categories &c
        List<CatInfo> infos = new ArrayList<CatInfo>();
        for (Category cat : data.categories) {
            infos.add(new CatInfo(cat.categoryId, Category.getHierarchy(cat, parents),
                                  cat.creator.toString()));
        }
        init(infos);
    }

    protected void init (List<CatInfo> infos) {
        _infos = infos;

        Collections.sort(infos, _mode.comp);
        final FluentTable contents = new FluentTable(2, 0);
        FluentTable.Cell cell = contents.add();
        if (_ctx.isAdmin()) cell = cell.setHTML("&nbsp;").right();
        cell.setWidget(Args.createLink("Series", Page.EDIT_INDEV, Mode.NAME), "machine").right().
            setWidget(Args.createLink("Creator", Page.EDIT_INDEV, Mode.CREATOR), "machine");
        for (final CatInfo info : infos) {
            cell = contents.add();
            if (_ctx.isAdmin()) {
                final Button delete = new Button("x");
                delete.addClickHandler(new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        // figure out what row this cat info is on (this may shift if we delete
                        // multiple rows, so we can't save it during init())
                        final int rowIdx = _infos.indexOf(info);
                        if (rowIdx < 0) return; // double click or something, abort!
                        _editorsvc.deleteCategory(info.categoryId, new PopupCallback<Void>(delete) {
                            public void onSuccess (Void unused) {
                                _infos.remove(rowIdx);
                                contents.removeRow(rowIdx+1); // account for header
                            }
                        });
                    }
                });
                cell = cell.setWidget(delete).right();
            }
            cell.setWidget(Args.createInlink(info.path, Page.EDIT_SERIES, info.categoryId)).right().
                setText(info.creator);
        }
        clear();
        add(contents);
    }

    protected enum Mode {
        NAME(new Comparator<CatInfo>() {
            public int compare (CatInfo one, CatInfo two) {
                return one.path.compareTo(two.path);
            }
        }),
        CREATOR(new Comparator<CatInfo>() {
            public int compare (CatInfo one, CatInfo two) {
                return one.creator.compareTo(two.creator);
            }
        });

        public final Comparator<CatInfo> comp;

        Mode (Comparator<CatInfo> comp) {
            this.comp = comp;
        }
    };

    protected static class CatInfo {
        public final int categoryId;
        public final String path;
        public final String creator;
        public CatInfo (int categoryId, String path, String creator) {
            this.categoryId = categoryId;
            this.path = path;
            this.creator = creator;
        }
    }

    protected Mode _mode;
    protected List<CatInfo> _infos;

    protected static final EditorServiceAsync _editorsvc = GWT.create(EditorService.class);
}
