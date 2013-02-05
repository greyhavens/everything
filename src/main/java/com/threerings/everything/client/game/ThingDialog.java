//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.everything.rpc.EverythingService;
import com.threerings.everything.rpc.EverythingServiceAsync;
import com.threerings.everything.rpc.Kontagent;
import com.threerings.everything.data.Build;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.PlayerName;
import com.threerings.gwt.util.Console;

import com.threerings.everything.client.util.Context;
import com.threerings.everything.client.util.KontagentUtil;
import com.threerings.everything.client.util.Page;

/**
 * Handles displaying our various Facebook bragging, wanting, gifting dialogs.
 */
public class ThingDialog
{
    /**
     * Creates a click handler that displays a "I got this card" dialog.
     * The specified callback will be called with 'true' if they ended up posting the story.
     */
    public static ClickHandler makeGotHandler (
        Context ctx, Card card, boolean completed, AsyncCallback<Boolean> callback)
    {
        if (completed) {
            return makeHandler(ctx, "got_comp", card,
                               "" + ctx.getMe() + " got the " + card.thing.name +
                                       " card and completed the " + card.getSeries() +
                                       " series in Everything!",
                               "Celebrate your completed series:", callback);

        } else if (card.giver == null) {
            return makeHandler(ctx, "got_card", card,
                               "" + ctx.getMe() + " got the " + card.thing.name +
                                   " card in Everything.",
                               "Tell your friends about this awesome card:", callback);

        } else if (card.giver.userId == Card.BIRTHDAY_GIVER_ID) {
            return makeHandler(ctx, "got_bgift", card,
                               "" + ctx.getMe() + " got the " + card.thing.name +
                                       " card as a birthday gift from Everything.",
                               "Celebrate being born:", callback);

        } else {
            return makeHandler(ctx, "got_gift", card,
                               "" + ctx.getMe() + " got the " + card.thing.name +
                                       " card from " + card.giver + " in Everything.",
                               "Thank your friend for this great gift:", callback);
        }
    }

    /**
     * Creates a click handler that displays a "I want this card" dialog.
     */
    public static ClickHandler makeWantHandler (Context ctx, Card card)
    {
        return makeHandler(ctx, "want_card", card,
                           "" + ctx.getMe() + " wants the " + card.thing.name +
                                   " card in Everything.",
                           "Let your friends know you want this card:", null);
    }

    /**
     * Displays a feed dialog reporting that a player gifted a card.
     */
    public static void showGifted (Context ctx, Card card, PlayerName target)
    {
        showDialog(ctx, "gave_card", ""+target.facebookId,
                   "" + ctx.getMe() + " gave the " + card.thing.name + " card to " + target +
                            " in Everything.",
                   card,
                   target, "Tell your friends why you gave " + target.name + " this card:", null);
    }

    /**
     * Displays a feed dialog for posting an "attractor" card to recruit new players.
     * The specified callback will be called with 'true' if they ended up posting the story.
     */
    public static void showAttractor (
        Context ctx, Card card, final AsyncCallback<Boolean> callback)
    {
        final String tracking = KontagentUtil.generateUniqueId(ctx.getMe().userId);
//        String joinURL = ctx.getEverythingURL(
        String joinURL = ctx.getFacebookAddURL(
            Kontagent.POST, tracking, Page.ATTRACTOR, card.thing.thingId, ctx.getMe().userId);
        String imageURL = GWT.getModuleBaseURL() + "cardimg?thing=" + card.thing.thingId;
        showDialog("", card.thing.name, card.thing.descrip + "\nFacts: " + card.thing.facts,
                   Category.getHierarchy(card.categories), card.thing.rarity.toString(),
                   imageURL, joinURL, joinURL, "Start your collection with this card!",
                   "Tell your friends why Everthing is fun:",
                   new Command() { // complete callback
                       public void execute () {
                           _everysvc.storyPosted(tracking, new AsyncCallback<Void>() {
                               public void onSuccess (Void result) { /* nada */ }
                               public void onFailure (Throwable cause) {
                                   Console.log("Failed to report story post",
                                       "tracking", tracking, cause);
                               }
                           });
                           callback.onSuccess(true);
                       }
                   },
                   new Command() { // incomplete callback
                       public void execute () {
                           callback.onSuccess(false);
                       }
                   });
    }

    protected static ClickHandler makeHandler (
        final Context ctx, final String vec, final Card card, final String title,
        final String prompt, final AsyncCallback<Boolean> callback)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                String targetId = (card.giver == null) ? null : (""+card.giver.facebookId);
                showDialog(ctx, vec, targetId, title, card, card.owner, prompt, callback);
            }
        };
    }

    protected static void showDialog (
        Context ctx, String vec, String targetId, String title, Card card, PlayerName owner,
        String prompt, final AsyncCallback<Boolean> callback)
    {
        final String tracking = KontagentUtil.generateUniqueId(ctx.getMe().userId);
        String cardURL = ctx.getEverythingURL(
            Kontagent.POST, tracking, Page.BROWSE, owner.userId, card.thing.categoryId);
        String everyURL = ctx.getEverythingURL(Kontagent.POST, tracking, Page.LANDING);
        String imageURL = GWT.getModuleBaseURL() + "cardimg?thing=" + card.thing.thingId;
        showDialog(targetId, title, card.thing.descrip,
                   Category.getHierarchy(card.categories), card.thing.rarity.toString(),
                   imageURL, cardURL, everyURL, "Start your own Everything collection!", prompt,
                   new Command() { // complete callback
                       public void execute () {
                           _everysvc.storyPosted(tracking, new AsyncCallback<Void>() {
                               public void onSuccess (Void result) { /* nada */ }
                               public void onFailure (Throwable cause) {
                                   Console.log("Failed to report story post",
                                       "tracking", tracking, cause);
                               }
                           });
                           if (callback != null) {
                               callback.onSuccess(true);
                           }
                       }
                   },
                   new Command() { // incomplete callback
                       public void execute () {
                           if (callback != null) {
                               callback.onSuccess(false);
                           }
                       }
                   });
    }

    protected static native void showDialog (String targetId, String title, String descrip,
                                             String categories, String rarity,
                                             String imageURL, String cardURL,
                                             String everyURL, String everyURLText,
                                             String prompt,
                                             Command onComplete, Command onIncomplete) /*-{
        var publish = {
            method: "stream.publish",
            attachment: {
                name: title,
                href: everyURL,
                description: descrip,
                media: [{ type: "image",
                          src: imageURL,
                          href: cardURL }],
                properties: {"Category": categories,
                             "Rarity": rarity,
                             "Join the fun": { text: everyURLText,
                                               href: everyURL }},
            },
            action_links: [{ text: "Collect Everything",
                             href: everyURL }],
            target_id: targetId,
            user_message_prompt: prompt, // TODO: this seems not to work
        };
        $wnd.FB.ui(publish, function (rsp) {
            if (rsp && rsp.post_id) {
                onComplete.@com.google.gwt.user.client.Command::execute()();
            } else {
                onIncomplete.@com.google.gwt.user.client.Command::execute()();
            }
        });
    }-*/;

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
}
