//
// $Id$

package com.threerings.everything.client.editor;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.rpc.EditorService;
import com.threerings.everything.rpc.EditorServiceAsync;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.PendingSeries;

import com.threerings.everything.client.ui.ButtonUI;
import com.threerings.everything.client.ui.DataPanel;
import com.threerings.everything.client.util.Args;
import com.threerings.everything.client.util.ClickCallback;
import com.threerings.everything.client.util.Context;
import com.threerings.everything.client.util.Page;

/**
 * Displays the series that are pending approval. Allows voting and comment leaving by other
 * editors.
 */
public class EditPendingPage extends DataPanel<List<PendingSeries>>
{
    public EditPendingPage (Context ctx)
    {
        super(ctx, "editPending");
        _editorsvc.loadPendingSeries(createCallback());
    }

    protected void init (List<PendingSeries> penders)
    {
        add(Widgets.newHTML(_msgs.pendingIntro(), "handwriting"));

        Collections.sort(penders, new Comparator<PendingSeries>() {
            public int compare (PendingSeries s1, PendingSeries s2) {
                return s2.categoryId - s1.categoryId; // this will never get near max_int/2
            }
        });

        FluentTable ptbl = new FluentTable(0, 5, "TopPadded");
        ptbl.add().setText(_msgs.pendingCat(), "machine").
            right().setText(_msgs.pendingSubcat(), "machine").
            right().setText(_msgs.pendingSeries(), "machine").
            right().setText(_msgs.pendingVotes(), "machine");

        for (final PendingSeries pender : penders) {
            ptbl.add().setText("", "Line").setColSpan(5);

            final PushButton vote = ButtonUI.newSmallButton(getVoteLabel(pender));
            final Label votes = Widgets.newLabel(repeat(CHECK_CHAR, pender.voters.size()));
            ptbl.add().setText(pender.category + " " + Category.SEP_CHAR).
                right().setText(pender.subcategory + " " + Category.SEP_CHAR).
                right().setWidget(
                    Args.createLink(pender.name, Page.EDIT_SERIES, pender.categoryId)).
                right().setWidget(votes).
                right().setWidget(pender.creatorId == _ctx.getMe().userId ?
                                  Widgets.newLabel("") : vote); // no voting for your own series

            new ClickCallback<Void>(vote) {
                protected boolean callService () {
                    _editorsvc.updatePendingVote(
                        pender.categoryId, !pender.voters.contains(_ctx.getMe().userId), this);
                    return true;
                }

                protected boolean gotResult (Void result) {
                    if (pender.voters.contains(_ctx.getMe().userId)) {
                        pender.voters.remove(_ctx.getMe().userId);
                        Popups.infoNear("Vote rescinded.", vote);
                    } else {
                        pender.voters.add(_ctx.getMe().userId);
                        Popups.infoNear("Vote submitted.", vote);
                    }
                    votes.setText(repeat(CHECK_CHAR, pender.voters.size()));
                    vote.getUpFace().setText(getVoteLabel(pender));
                    return true;
                }
            };
        };
        add(ptbl);

        if (penders.size() == 0) {
            add(Widgets.newLabel(_msgs.pendingNone(), "TopPadded", "machine"));
        } else {
            add(Widgets.newLabel(_msgs.pendingPolicy(), "TopPadded", "handwriting"));
        }
    }

    protected String getVoteLabel (PendingSeries pender)
    {
        return pender.voters.contains(_ctx.getMe().userId) ?
            _msgs.pendingUnvote() : _msgs.pendingVote();
    }

    protected static final String repeat (String text, int count)
    {
        return (count == 0) ? "" : text + repeat(text, count-1);
    }

    protected static final String CHECK_CHAR = "\u2714";

    protected static final EditorMessages _msgs = GWT.create(EditorMessages.class);
    protected static final EditorServiceAsync _editorsvc = GWT.create(EditorService.class);
}
