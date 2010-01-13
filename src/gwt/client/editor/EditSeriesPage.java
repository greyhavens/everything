//
// $Id$

package client.editor;

import java.util.Date;
import java.util.List;

import com.google.common.base.Function;
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
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Bindings;
import com.threerings.gwt.ui.DefaultTextListener;
import com.threerings.gwt.ui.EnumListBox;
import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.LimitedTextArea;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.Value;

import com.threerings.everything.client.EditorService;
import com.threerings.everything.client.EditorServiceAsync;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.CategoryComment;
import com.threerings.everything.data.Rarity;
import com.threerings.everything.data.Thing;

import client.game.CardView;
import client.ui.ButtonUI;
import client.ui.DataPanel;
import client.util.Args;
import client.util.ClickCallback;
import client.util.Context;
import client.util.MediaUploader;
import client.util.Messages;
import client.util.PopupCallback;

/**
 * Displays an interface for editing a particular series.
 */
public class EditSeriesPage extends DataPanel<EditorService.SeriesResult>
{
    public EditSeriesPage (Context ctx, int categoryId)
    {
        super(ctx, "page", "editSeries");
        _editorsvc.loadSeries(categoryId, createCallback());
    }

    @Override // from DataPanel
    protected void init (final EditorService.SeriesResult result)
    {
        final Value<Category> series = Value.create(result.categories[result.categories.length-1]);
        final Value<Boolean> editable = series.map(new Function<Category, Boolean>() {
            public Boolean apply (Category series) {
                return _ctx.isAdmin() ||
                    (series.isInDevelopment() && _ctx.getMe().equals(series.creator));
            }
        });

        // add some metadata at the top
        // TODO: make this a ValueLabel
        add(Widgets.newLabel(Category.getHierarchy(result.categories), "Header", "handwriting"));

        FluentTable info = new FluentTable(5, 0, "handwriting");
        add(info);
        info.add().setText("Creator:").right().setWidget(Args.createInlink(series.get().creator));
        info.add().setText("Seed ID:").right().setText(series.get().categoryId);

        if (_ctx.isAdmin() || _ctx.getMe().equals(series.get().creator)) {
            final TextBox name = Widgets.newTextBox(
                series.get().name, Category.MAX_NAME_LENGTH, 15);
            Button update = new Button("Update");
            Bindings.bindEnabled(editable, name, update);
            new ClickCallback<Void>(update, name) {
                protected boolean callService () {
                    series.get().name = name.getText().trim();
                    _editorsvc.updateCategory(series.get(), this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    series.update(series.get());
                    Popups.infoNear("Name updated.", name);
                    return true;
                }
            };
            info.add().setText("Name:").right().setWidget(Widgets.newRow(name, update));
        }

        final EnumListBox<Category.State> state =
            new EnumListBox<Category.State>(Category.State.class) {
            @Override protected String toLabel (Category.State value) {
                return Messages.xlate(value.toString());
            }
        };
        series.addListener(new Value.Listener<Category>() {
            public void valueChanged (Category series) {
                state.setSelectedValue(series.state);
            }
        });
        final ChangeHandler onStateChange = new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                if (series.get().state == state.getSelectedValue()) {
                    return;
                }
                series.get().state = state.getSelectedValue();
                _editorsvc.updateCategory(series.get(), new PopupCallback<Void>() {
                    public void onSuccess (Void result) {
                        series.update(series.get());
                        switch (series.get().state) {
                        case IN_DEVELOPMENT: Popups.infoNear("Series is editable.", state); break;
                        case PENDING_REVIEW: Popups.infoNear("Submitted for review.", state); break;
                        case ACTIVE: Popups.infoNear("Series activated.", state); break;
                        }
                    }
                });
            }
        };
        state.addChangeHandler(onStateChange);
        state.setEnabled(_ctx.isAdmin());
        final Button chstate = new Button("", new ClickHandler() {
            public void onClick (ClickEvent event) {
                if (series.get().state == Category.State.PENDING_REVIEW) {
                    state.setSelectedValue(Category.State.IN_DEVELOPMENT);
                } else {
                    state.setSelectedValue(Category.State.PENDING_REVIEW);
                }
                onStateChange.onChange(null); // annoyingly setSelectedValue doesn't do this
            }
        });
        series.addListener(new Value.Listener<Category>() {
            public void valueChanged (Category series) {
                switch (series.state) {
                case IN_DEVELOPMENT: chstate.setText("Submit for review"); break;
                case PENDING_REVIEW: chstate.setText("Cancel review"); break;
                }
                chstate.setVisible(_ctx.getMe().equals(series.creator) && !series.isActive());
            }
        });
        info.add().setText("State:").right().setWidget(Widgets.newRow(state, chstate));

        final TextBox message = Widgets.newTextBox("", 255, 35);
        final Button post = new Button("Add");
        info.add().setText("Comments:").right().setWidget(Widgets.newRow(message, post));

        final FlowPanel comments = new FlowPanel();
        info.add().setWidget(comments).setColSpan(2);
        showComments(comments, result.comments, false);

        new ClickCallback<CategoryComment>(post, message) {
            protected boolean callService () {
                String text = message.getText().trim();
                if (text.length() == 0) {
                    return false;
                }
                _editorsvc.postComment(series.get().categoryId, text, this);
                return true;
            }
            protected boolean gotResult (CategoryComment comment) {
                if (comments.getWidget(0) instanceof Label) {
                    comments.clear(); // we were showing "no comments"
                }
                result.comments.add(0, comment);
                comments.insert(formatComment(comment), 0);
                message.setText("");
                return true;
            }
        };

        // then add all of our things
        int position = 0;
        for (Thing thing : result.things) {
            // create a fake card and display it
            add(new ThingEditor(editable, createCard(result, thing, position++)));
        }

        // finally add a UI for creating new things
        add(Widgets.newHTML("Add Things", "Header", "machine"));
        final TextBox thing = Widgets.newTextBox("", Thing.MAX_NAME_LENGTH, 15);
        DefaultTextListener.configure(thing, "<new thing name>");
        final Button create = new Button("Add Thing");
        new ClickCallback<Integer>(create, thing) {
            protected boolean callService () {
                String text = thing.getText().trim();
                if (text.length() == 0) {
                    return false;
                }
                _thing = createBlankThing(text, series.get().categoryId);
                _editorsvc.createThing(_thing, this);
                return true;
            }

            protected boolean gotResult (Integer thingId) {
                _ctx.getCatsModel().thingAdded(series.get());
                thing.setText("");
                _thing.thingId = thingId;
                result.things.add(_thing);
                int position = result.things.size()-1;
                ThingEditor editor = new ThingEditor(
                    editable, createCard(result, _thing, position));
                insert(editor, getWidgetCount()-2);
                editor.setEditing(true);
                _thing = null;
                return true;
            }

            protected Thing _thing;
        };
        add(Widgets.newRow(thing, create));
        Bindings.bindEnabled(editable, thing, create);

        // only allow adding new items if the series is active
        series.update(series.get());
    }

    protected Card createCard (EditorService.SeriesResult result, Thing thing, int position)
    {
        Card card = new Card();
        card.owner = _ctx.getMe();
        card.categories = result.categories;
        card.thing = thing;
        card.position = position;
        card.things = result.things.size();
        card.giver = _ctx.getMe();
        card.received = new Date();
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
        thing.source = "";
        return thing;
    }

    protected Widget formatComment (CategoryComment comment)
    {
        return Widgets.newRow(
            Widgets.newInlineLabel(DateUtil.formatDateTime(comment.when), "nowrap"),
            Args.createInlink(comment.commentor),
            Widgets.newInlineLabel(comment.message));
    }

    protected void showComments (
        final FlowPanel combox, final List<CategoryComment> comments, final boolean all)
    {
        combox.clear();
        int shown = 0;
        for (CategoryComment comment : comments) {
            combox.add(formatComment(comment));
            if (!all && ++shown >= BRIEF_COMMENT_COUNT) {
                break;
            }
        }
        if (comments.size() > BRIEF_COMMENT_COUNT) {
            String label = all ? "[show recent]" : "[show all]";
            combox.add(Widgets.newActionLabel(label, "tipLabel", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    showComments(combox, comments, !all);
                }
            }));
        } else if (comments.size() == 0) {
            combox.add(Widgets.newLabel("No comments."));
        }
    }

    protected class ThingEditor extends FlowPanel
    {
        public ThingEditor (final Value<Boolean> editable, final Card card) {
            setStyleName("Editor");

            editable.addListener(new Value.Listener<Boolean>() {
                public void valueChanged (Boolean editable) {
                    _edit.setEnabled(editable);
                }
            });

            _ctrl = new FluentTable(5, 0);
            final TextBox name = Widgets.newTextBox(card.thing.name, Thing.MAX_NAME_LENGTH, 15);
            _ctrl.add().setText("Name", "right").right().setWidget(name);

            final EnumListBox<Rarity> rarity = new EnumListBox<Rarity>(Rarity.class);
            rarity.setSelectedValue(card.thing.rarity);
            rarity.addChangeHandler(new ChangeHandler() {
                public void onChange (ChangeEvent event) {
                    card.thing.rarity = rarity.getSelectedValue();
                    updateCard(card);
                }
            });
            _ctrl.add().setText("Rarity", "right").right().setWidget(rarity).setColSpan(3);

            MediaUploader uploader = new MediaUploader(new MediaUploader.Listener() {
                public void mediaUploaded (String image) {
                    card.thing.image = image;
                    updateCard(card);
                }
            });
            String uptip = "Upload an image from your computer";
            final TextBox imgurl = Widgets.newTextBox("", -1, 25);
            DefaultTextListener.configure(imgurl, "<paste image url, press enter>");
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
            String imgtip = "Or slurp one directly from the Internets";
            _ctrl.add().setText("Image", "right").right().
                setWidgets(uploader, Widgets.newLabel(uptip, "tipLabel")).right().
                setWidgets(imgurl, Widgets.newLabel(imgtip, "tipLabel")).setColSpan(2);

            final LimitedTextArea descrip = Widgets.newTextArea(
                card.thing.descrip, -1, 2, Thing.MAX_DESCRIP_LENGTH);
            _ctrl.add().setText("Descrip", "right").right().
                setWidget(descrip, "Wide").setColSpan(3);

            final LimitedTextArea facts = Widgets.newTextArea(
                card.thing.facts, -1, 4, Thing.MAX_FACTS_LENGTH);
            _ctrl.add().setText("Facts", "right").right().setWidget(facts, "Wide").setColSpan(3);

            final TextBox source = Widgets.newTextBox(card.thing.source, 255, -1);
            final String defsource = "<source URL, e.g. http://en.wikipedia.org/wiki/Everything>";
            DefaultTextListener.configure(source, defsource);
            _ctrl.add().setText("Source", "right").right().setWidget(source, "Wide").setColSpan(3);

            Button cancel = new Button("Cancel", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    setEditing(false);
                }
            });
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
                    setEditing(false);
                    Popups.infoNear("Thing saved.", _edit);
                    return true;
                }
            };
            _ctrl.add().right().setWidget(Widgets.newRow(cancel, delete, save)).
                setColSpan(3).alignRight();
                    
            final Timer updater = new Timer() {
                @Override public void run () {
                    card.thing.name = name.getText().trim();
                    card.thing.descrip = descrip.getText().trim();
                    card.thing.facts = facts.getText().trim();
                    card.thing.source = DefaultTextListener.getText(source, defsource);
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

        public void setEditing (boolean editing)
        {
            remove(_ctrl);
            if (editing) {
                add(_ctrl);
            }
            _edit.setEnabled(!editing);
        }

        protected void updateCard (Card card) {
            if (getWidgetCount() > 0) {
                remove(0);
            }
            insert(CardView.create(_ctx, card, false, null, null, _edit), 0);
        }

        protected FluentTable _ctrl;
        protected PushButton _edit = ButtonUI.newButton("Edit", new ClickHandler() {
            public void onClick (ClickEvent event) {
                setEditing(true);
            }
        });
    }

    protected static final EditorServiceAsync _editorsvc = GWT.create(EditorService.class);
    protected static final int BRIEF_COMMENT_COUNT = 3;
}
