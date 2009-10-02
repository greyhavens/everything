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
            return makeHandler(ctx, "got_comp", Build.Template.GOT_COMP.id, card,
                               "Celebrate your completed series:", "Woo hoo!");
        } else if (card.giver != null) {
            return makeHandler(ctx, "got_gift", Build.Template.GOT_GIFT.id, card,
                               "Thank your friend for this great gift:", "Thanks!");
        } else {
            return makeHandler(ctx, "got_card", Build.Template.GOT_CARD.id, card,
                               "Tell your friends about this awesome card:", "Woo!");
        }
    }

    /**
     * Creates a click handler that displays a "I want this card" dialog.
     */
    public static ClickHandler makeWantHandler (Context ctx, Card card)
    {
        return makeHandler(ctx, "want_card", Build.Template.WANT_CARD.id, card,
                           "Let your friends know you want this card:", "I wants it!");
    }

    /**
     * Displays a feed dialog reporting that a player gifted a card.
     */
    public static void showGifted (Context ctx, Card card, PlayerName target)
    {
        showDialog(ctx, "gave_card", Build.Template.GAVE_CARD.id, ""+target.facebookId, card,
                   target, "Tell your friends why you gave " + target.name + " this card:", "");
    }

    protected static ClickHandler makeHandler (final Context ctx, final String vec,
                                               final String templateId, final Card card,
                                               final String prompt, final String message)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                String targetId = (card.giver == null) ? null : (""+card.giver.facebookId);
                showDialog(ctx, vec, templateId, targetId, card, card.owner, prompt, message);
            }
        };
    }

    protected static void showDialog (Context ctx, String vec, String templateId, String targetId,
                                      Card card, PlayerName owner, String prompt, String message)
    {
        final String tracking = KontagentUtil.generateUniqueId(ctx.getMe().userId);
        String cardURL = ctx.getEverythingURL(
            Kontagent.POST, tracking, Page.BROWSE, owner.userId, card.thing.categoryId);
        String everyURL = ctx.getEverythingURL(Kontagent.POST, tracking, Page.LANDING);
        String imageURL = GWT.getModuleBaseURL() + "cardimg?thing=" + card.thing.thingId;
        showDialog(templateId, targetId, card.thing.name, card.thing.descrip,
                   Category.getHierarchy(card.categories), card.getSeries().name,
                   card.thing.rarity.toString(), imageURL,
                   cardURL, everyURL, prompt, message, new Command() {
            public void execute () {
                _everysvc.storyPosted(tracking, new AsyncCallback<Void>() {
                    public void onSuccess (Void result) { /* nada */ }
                    public void onFailure (Throwable cause) {
                        Console.log("Failed to report story post", "tracking", tracking, cause);
                    }
                });
            }
        });
    }

    protected static native void showDialog (String templateId, String targetId, String thing,
                                             String descrip, String categories, String series,
                                             String rarity, String image, String cardURL,
                                             String everyURL, String prompt, String message,
                                             Command onComplete) /*-{
        var data = {
            'thing': thing,
            'descrip': descrip,
            'category': categories,
            'series': series,
            'rarity': rarity,
            'cardURL': cardURL,
            'everyURL': everyURL,
            'images': [{ 'src': image, 'href': cardURL }],
        };
        var target = (targetId == null) ? null : [ targetId ];
        var msg = { value: message };
        $wnd.FB.Connect.showFeedDialog(templateId, data, target, null, null, null, function () {
            // we used to try to send msg.value to this method but Facebook doesn't actually update
            // it with the changed text, at least not in any of the numerous tests I did; awesome!
            // not that it would matter because we still don't know for sure one way or the other
            // if the user clicked Post or Skip because Facebook won't tell us that information,
            // because we "can't be trusted with it"
            onComplete.@com.google.gwt.user.client.Command::execute()();
        }, prompt, msg);
    }-*/;

// We used to use streamPublish, which is *way* simpler but does not allow us to highlight text in
// the title which is critical for making our feed stories not look like random news articles
//
//     protected static native void showDialog (String message, String thing, String descrip,
//                                              String category, String rarity, String image,
//                                              String url) /*-{
//         var attachment = {
//             'name': thing,
//             'href': url,
//             'description': descrip,
//             'media': [{ 'type': 'image',
//                         'src': image,
//                         'href': url }],
//             'properties': {'Category': category,
//                            'Rarity': rarity,
//                            'Join the fun': {'text': 'Play Everything!',
//                                             'href': 'http://apps.facebook.com/everythinggame/'}},
//         };
//         var actions = [{ "text": "Play Everything",
//                          "href": "http://apps.facebook.com/everythinggame/"}];
//         $wnd.FB.Connect.streamPublish(message, attachment, actions);
//     }-*/;

    protected static final EverythingServiceAsync _everysvc = GWT.create(EverythingService.class);
}
