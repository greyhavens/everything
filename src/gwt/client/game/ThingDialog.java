//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.everything.client.EverythingService;
import com.threerings.everything.client.EverythingServiceAsync;
import com.threerings.everything.client.Kontagent;
import com.threerings.everything.data.Build;
import com.threerings.everything.data.Card;
import com.threerings.everything.data.Category;
import com.threerings.everything.data.PlayerName;
import com.threerings.gwt.util.Console;

import client.util.Context;
import client.util.KontagentUtil;
import client.util.Page;

/**
 * Handles displaying our various Facebook bragging, wanting, gifting dialogs.
 */
public class ThingDialog
{
    /**
     * Creates a click handler that displays a "I got this card" dialog.
     */
    public static ClickHandler makeGotHandler (Context ctx, Card card, boolean completed)
    {
        if (completed) {
            return makeHandler(ctx, "got_comp", card,
                               "" + ctx.getMe() + " got the " + card.thing.name +
                                       " card and completed the " + card.getSeries() +
                                       " series in Everything!",
                               "Celebrate your completed series:", "Woo hoo!");
        } else if (card.giver != null) {
            return makeHandler(ctx, "got_gift", card,
                               "" + ctx.getMe() + " got the " + card.thing.name +
                                       " card from " + card.giver + " in Everything.",
                               "Thank your friend for this great gift:", "Thanks!");
        } else {
            return makeHandler(ctx, "got_card", card,
                               "" + ctx.getMe() + " got the " + card.thing.name +
                                       " card in Everything.",
                               "Tell your friends about this awesome card:", "Woo!");
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
                           "Let your friends know you want this card:", "I wants it!");
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
                   target, "Tell your friends why you gave " + target.name + " this card:", "",
                   null);
    }

    /**
     * Displays a feed dialog for posting an "attractor" card to recruit new players.
     * The specified callback will be called with 'true' if they ended up posting the story.
     */
    public static void showAttractor (
        Context ctx, Card card, String title, String message, AsyncCallback<Boolean> callback)
    {
        // TODO: owner: probably not use us, we don't have an owner..
        // TODO: we need to just call the javascript version directly so we can customize more
        showDialog(ctx, "attractor", null, title, card, ctx.getMe(),
            "Let your friends know about Everything fun:", message, callback);
    }

    protected static ClickHandler makeHandler (final Context ctx, final String vec,
                                               final Card card, final String title,
                                               final String prompt, final String message)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                String targetId = (card.giver == null) ? null : (""+card.giver.facebookId);
                showDialog(ctx, vec, targetId, title, card, card.owner, prompt, message, null);
            }
        };
    }

    protected static void showDialog (Context ctx, String vec, String targetId, String title,
                                      Card card, PlayerName owner, String prompt, String message,
                                      final AsyncCallback<Boolean> optCallback)
    {
        final String tracking = KontagentUtil.generateUniqueId(ctx.getMe().userId);
        String cardURL = ctx.getEverythingURL(
            Kontagent.POST, tracking, Page.BROWSE, owner.userId, card.thing.categoryId);
        String everyURL = ctx.getEverythingURL(Kontagent.POST, tracking, Page.LANDING);
        String imageURL = GWT.getModuleBaseURL() + "cardimg?thing=" + card.thing.thingId;
        showDialog(targetId, title, card.thing.descrip,
                   Category.getHierarchy(card.categories), card.thing.rarity.toString(),
                   imageURL, cardURL, everyURL, prompt, message,
                   new Command() { // complete callback
                       public void execute () {
                           _everysvc.storyPosted(tracking, new AsyncCallback<Void>() {
                               public void onSuccess (Void result) { /* nada */ }
                               public void onFailure (Throwable cause) {
                                   Console.log("Failed to report story post",
                                       "tracking", tracking, cause);
                               }
                           });
                           if (optCallback != null) {
                               optCallback.onSuccess(Boolean.TRUE);
                           }
                       }
                   },
                   new Command() { // incomplete callback
                       public void execute () {
                           if (optCallback != null) {
                               optCallback.onSuccess(Boolean.FALSE);
                           }
                       }
                   });
    }

    protected static native void showDialog (String targetId, String title, String descrip,
                                             String categories, String rarity,
                                             String imageURL, String cardURL, String everyURL,
                                             String prompt, String message,
                                             Command onComplete, Command onIncomplete) /*-{
        var attachment = {
            'name': title,
            'href': everyURL,
            'description': descrip,
            'media': [{ 'type': 'image',
                        'src': imageURL,
                        'href': cardURL }],
            'properties': {'Category': categories,
                           'Rarity': rarity,
                           'Join the fun': {'text': 'Start your own Everything collection!',
                                            'href': everyURL }},
        };
        var actions = [{ "text": "Collect Everything",
                         "href": everyURL }];
        $wnd.FB.Connect.streamPublish(message, attachment, actions, targetId, prompt,
            function (postId, exception) {
                if ((postId != null) && (postId != "null")) { // holy shit, what a fiasco
                    onComplete.@com.google.gwt.user.client.Command::execute()();
                } else {
                    onIncomplete.@com.google.gwt.user.client.Command::execute()();
                }
            }
        );
    }-*/;

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
}
