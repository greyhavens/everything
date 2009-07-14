//
// $Id$

package client.editor;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
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
import client.util.PanelCallback;

/**
 * Displays an interface for editing a particular series.
 */
public class EditSeriesPanel extends FlowPanel
{
    public EditSeriesPanel (Context ctx, int categoryId)
    {
        setStyleName("editSeries");
        add(Widgets.newLabel("Loading...", "infoLabel"));

        _ctx = ctx;
        _editorsvc.loadSeries(categoryId, new PanelCallback<EditorService.SeriesResult>(this) {
            public void onSuccess (EditorService.SeriesResult result) {
                clear();
                init(result);
            }
        });
    }

    protected void init (final EditorService.SeriesResult result)
    {
        // add some metadata at the top
        final Category series = result.categories[result.categories.length-1];
        add(Widgets.newHTML(Category.getHierarchyHTML(result.categories), "Title"));

        SmartTable info = new SmartTable(5, 0);
        add(info);
        int row = 0;
        info.setText(row, 0, "Creator:");
        info.setWidget(row++, 1, Args.createLink(series.creator.toString(), Page.BROWSE,
                                                 series.creator.userId));

        if (_ctx.isAdmin() || _ctx.getMe().userId == series.getCreatorId()) {
            final TextBox name = Widgets.newTextBox(series.name, Category.MAX_NAME_LENGTH, 15);
            Button update = new Button("Update");
            info.setText(row, 0, "Name:");
            info.setWidget(row, 1, name);
            info.setWidget(row++, 2, update);
            new ClickCallback<Void>(update, name) {
                protected boolean callService () {
                    series.name = name.getText().trim();
                    _editorsvc.updateCategory(series, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    Popups.infoNear("Name updated.", name);
                    return true;
                }
            };
        }

        final CheckBox active = new CheckBox();
        active.setValue(series.active);
        active.setEnabled(_ctx.isAdmin());
        info.setText(row, 0, "Activated:");
        info.setWidget(row++, 1, active);
        if (_ctx.isAdmin()) {
            new ClickCallback<Void>(active) {
                protected boolean callService () {
                    series.active = active.getValue();
                    _editorsvc.updateCategory(series, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    String msg = series.active ? "Category activated." : "Category deactivated.";
                    Popups.infoNear(msg, active);
                    updateActive(series);
                    return true;
                }
            };
        }

        // then add all of our things
        for (Thing thing : result.things) {
            // create a fake card and display it
            add(new ThingEditor(createCard(result.categories, thing)));
        }

        // finally add a UI for creating new things
        final TextBox thing = Widgets.newTextBox("", Thing.MAX_NAME_LENGTH, 15);
        DefaultTextListener.configure(thing, "<new thing name>");
        Button create = new Button("Add Thing");
        new ClickCallback<Integer>(create, thing) {
            protected boolean callService () {
                String text = thing.getText().trim();
                if (text.length() == 0) {
                    return false;
                }
                _thing = createBlankThing(text, series.categoryId);
                _editorsvc.createThing(_thing, this);
                return true;
            }

            protected boolean gotResult (Integer thingId) {
                _thing.thingId = thingId;
                thing.setText("");
                ThingEditor editor = new ThingEditor(createCard(result.categories, _thing));
                insert(editor, getWidgetCount()-1);
                editor.setEditing(true);
                _thing = null;
                return true;
            }

            protected Thing _thing;
        };
        add(Widgets.newRow(thing, create));

        // only allow adding new items if the series is active
        updateActive(series);
    }

    protected void updateActive (Category series)
    {
        boolean editable = !series.active &&
            (series.getCreatorId() == _ctx.getMe().userId || _ctx.isAdmin());
        for (int ii = 0; ii < getWidgetCount(); ii++) {
            Widget child = getWidget(ii);
            if (child instanceof ThingEditor) {
                ((ThingEditor)child).setEditable(editable);
            } else if (child instanceof TextBox) {
                ((TextBox)child).setEnabled(editable);
            }
        }
    }

    protected Card createCard (Category[] categories, Thing thing)
    {
        Card card = new Card();
        card.owner = _ctx.getMe();
        card.categories = categories;
        card.thing = thing;
        card.created = new Date();
        return card;
    }

    protected Thing createBlankThing (String name, int categoryId)
    {
        Thing thing = new Thing();
        thing.name = name;
        thing.categoryId = categoryId;
        thing.rarity = Rarity.I;
        thing.image = "";
        thing.descrip = "Briefly describe your thing here. Scroll down to make sure " +
            "your description and facts fit on the Thing display.";
        thing.facts = "Enter facts about your thing here.\nPress enter to end one " +
            "bullet point and start the next.";
        thing.source = "http://wikipedia.org/Todo";
        return thing;
    }

    protected class ThingEditor extends FlowPanel
    {
        public ThingEditor (final Card card) {
            setStyleName("Editor");

            int row = 0;
            _ctrl = new SmartTable(5, 0);
            _ctrl.setText(row, 0, "Name", 1, "right");
            final TextBox name = Widgets.newTextBox(card.thing.name, Thing.MAX_NAME_LENGTH, 15);
            _ctrl.setWidget(row, 1, name);

            _ctrl.setText(row, 2, "Rarity", 1, "right");
            final EnumListBox<Rarity> rarity = new EnumListBox<Rarity>(Rarity.class);
            rarity.setSelectedValue(card.thing.rarity);
            _ctrl.setWidget(row++, 3, rarity);
            rarity.addChangeHandler(new ChangeHandler() {
                public void onChange (ChangeEvent event) {
                    card.thing.rarity = rarity.getSelectedValue();
                    updateCard(card);
                }
            });

            _ctrl.setText(row, 0, "Image", 1, "right");
            _ctrl.setWidget(row, 1, new MediaUploader(new MediaUploader.Listener() {
                public void mediaUploaded (String image) {
                    card.thing.image = image;
                    updateCard(card);
                }
            }));
            final TextBox imgurl = Widgets.newTextBox("", -1, 25);
            DefaultTextListener.configure(imgurl, "<paste image url, press enter>");
            _ctrl.setWidget(row++, 2, imgurl, 2, null);
            new ClickCallback<String>(new Button("dummy"), imgurl) {
                protected boolean callService () {
                    _editorsvc.slurpImage(imgurl.getText().trim(), this);
                    return true;
                }
                protected boolean gotResult (String image) {
                    card.thing.image = image;
                    updateCard(card);
                    imgurl.setText("");
                    imgurl.setFocus(false);
                    return true;
                }
            };

            _ctrl.setText(row, 0, "Descrip", 1, "right");
            final LimitedTextArea descrip = Widgets.newTextArea(
                card.thing.descrip, -1, 2, Thing.MAX_DESCRIP_LENGTH);
            _ctrl.setWidget(row++, 1, descrip, 3, "Wide");

            _ctrl.setText(row, 0, "Facts", 1, "right");
            final LimitedTextArea facts = Widgets.newTextArea(
                card.thing.facts, -1, 4, Thing.MAX_FACTS_LENGTH);
            _ctrl.setWidget(row++, 1, facts, 3, "Wide");

            _ctrl.setText(row, 0, "Source", 1, "right");
            final TextBox source = Widgets.newTextBox(card.thing.source, 255, -1);
            _ctrl.setWidget(row++, 1, source, 3, "Wide");

            Button delete = new Button("Delete");
            new ClickCallback<Void>(delete) {
                protected boolean callService () {
                    _editorsvc.deleteThing(card.thing.thingId, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    ((FlowPanel)getParent()).remove(ThingEditor.this);
                    Popups.info("Thing deleted.");
                    return false;
                }
            }.setConfirmText("Are you sure you want to delete this thing?");
            final Button save = new Button("Save");
            new ClickCallback<Void>(save) {
                protected boolean callService () {
                    _editorsvc.updateThing(card.thing, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    Popups.infoNear("Thing saved.", save);
                    setEditing(false);
                    return true;
                }
            };
            Button done = new Button("Done", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    // TODO: check for unsaved modifications
                    setEditing(false);
                }
            });
            _ctrl.getFlexCellFormatter().setHorizontalAlignment(row, 1, HasAlignment.ALIGN_RIGHT);
            _ctrl.setWidget(row++, 1, Widgets.newRow(delete, save, done), 3, null);
                    
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

            add(_edit = Widgets.newActionLabel("Edit", "Edit", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    setEditing(true);
                }
            }));
        }

        public void setEditable (boolean editable)
        {
            remove(_ctrl);
            remove(_edit);
            if (editable) {
                add(_edit);
            }
        }

        public void setEditing (boolean editing)
        {
            remove(_ctrl);
            remove(_edit);
            if (editing) {
                add(_ctrl);
            } else {
                add(_edit);
            }
        }

        protected void updateCard (Card card) {
            if (getWidgetCount() > 0) {
                remove(0);
            }
            insert(CardView.create(card), 0);
        }

        protected SmartTable _ctrl;
        protected Widget _edit;
    }

    protected Context _ctx;

    protected static final EditorServiceAsync _editorsvc = GWT.create(EditorService.class);
}
