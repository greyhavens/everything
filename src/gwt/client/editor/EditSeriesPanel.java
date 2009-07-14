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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

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
import client.util.Context;
import client.util.MediaUploader;
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

    protected void init (EditorService.SeriesResult result)
    {
//         @Override protected void initItemRow (final Row<Category> row, boolean selected) {
//             super.initItemRow(row, selected);
//             // only admins get a checkbox to activate/deactivate a category
//             if (!_ctx.isAdmin()) {
//                 return;
//             }
//             row.add(Widgets.newShim(5, 5));
//             final CheckBox active = new CheckBox();
//             row.add(active);
//             active.setTitle("Series Active");
//             active.setValue(row.item.active);
//             active.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
//                 public void onValueChange (ValueChangeEvent<Boolean> event) {
//                     row.item.active = event.getValue();
//                     active.setEnabled(false);
//                     _editorsvc.updateCategory(row.item, new PopupCallback<Void>() {
//                         public void onSuccess (Void result) {
//                             String msg = row.item.active ?
//                                 "Category activated." : "Category deactivated.";
//                             Popups.infoNear(msg, active);
//                             active.setEnabled(true);
//                             _child.load(); // reload the thing list which will re-en/disable
//                         }
//                     });
//                 }
//             });
//         }

        add(Widgets.newHTML(Category.getHierarchyHTML(result.categories), "Title"));

        for (Thing thing : result.things) {
            // create a fake card and display it
            Card card = new Card();
            card.owner = _ctx.getMe();
            card.categories = result.categories;
            card.thing = thing;
            card.created = new Date();
            add(new ThingEditor(card));
        }
    }

    protected class ThingEditor extends FlowPanel
    {
        public ThingEditor (final Card card) {
            setStyleName("Editor");

            int row = 0;
            _bits = new SmartTable(5, 0);
            _bits.setText(row, 0, "Name", 1, "right");
            final TextBox name = Widgets.newTextBox(card.thing.name, Thing.MAX_NAME_LENGTH, 15);
            _bits.setWidget(row, 1, name);

            _bits.setText(row, 2, "Rarity", 1, "right");
            final EnumListBox<Rarity> rarity = new EnumListBox<Rarity>(Rarity.class);
            rarity.setSelectedValue(card.thing.rarity);
            _bits.setWidget(row++, 3, rarity);
            rarity.addChangeHandler(new ChangeHandler() {
                public void onChange (ChangeEvent event) {
                    card.thing.rarity = rarity.getSelectedValue();
                    updateCard(card);
                }
            });

            _bits.setText(row, 0, "Image", 1, "right");
            _bits.setWidget(row++, 1, new MediaUploader(new MediaUploader.Listener() {
                public void mediaUploaded (String name) {
                    card.thing.image = name;
                    updateCard(card);
                }
            }));

            _bits.setText(row, 0, "Descrip", 1, "right");
            final LimitedTextArea descrip = Widgets.newTextArea(
                card.thing.descrip, -1, 2, Thing.MAX_DESCRIP_LENGTH);
            _bits.setWidget(row++, 1, descrip, 3, "Wide");

            _bits.setText(row, 0, "Facts", 1, "right");
            final LimitedTextArea facts = Widgets.newTextArea(
                card.thing.facts, -1, 4, Thing.MAX_FACTS_LENGTH);
            _bits.setWidget(row++, 1, facts, 3, "Wide");

            _bits.setText(row, 0, "Source", 1, "right");
            final TextBox source = Widgets.newTextBox(card.thing.source, 255, -1);
            _bits.setWidget(row++, 1, source, 3, "Wide");

            final Button save = new Button("Save");
            new ClickCallback<Void>(save) {
                protected boolean callService () {
                    _editorsvc.updateThing(card.thing, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    Popups.infoNear("Thing saved.", save);
                    return true;
                }
            };
            Button done = new Button("Done", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    // TODO: check for unsaved modifications
                    remove(_bits);
                    add(_edit);
                }
            });
            _bits.getFlexCellFormatter().setHorizontalAlignment(row, 1, HasAlignment.ALIGN_RIGHT);
            _bits.setWidget(row++, 1, Widgets.newRow(save, done), 3, null);
                    
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
                    remove(_edit);
                    add(_bits);
                }
            }));
        }

        protected void updateCard (Card card) {
            if (getWidgetCount() > 0) {
                remove(0);
            }
            insert(CardView.create(card), 0);
        }

        protected SmartTable _bits;
        protected Widget _edit;
    }

    protected Context _ctx;

    protected static final EditorServiceAsync _editorsvc = GWT.create(EditorService.class);
}
