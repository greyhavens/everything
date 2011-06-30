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
import com.threerings.everything.data.TrophyData;
import com.threerings.gwt.util.Console;

import client.util.Context;
import client.util.KontagentUtil;
import client.util.Page;

/**
 * Handles displaying a Facebook brag dialog for winning a trophy.
 */
public class TrophyDialog
{
    public static ClickHandler makeHandler (final Context ctx, final TrophyData trophy)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                showDialog(ctx, trophy);
            }
        };
    }

    protected static void showDialog (Context ctx, TrophyData trophy)
    {
        final String tracking = KontagentUtil.generateUniqueId(ctx.getMe().userId);

        String title = ctx.getMe() + " got the '" + trophy.name + "' trophy in Everything.";
        String desc = "Through persistent collecting, " + ctx.getMe() + " has earned " +
            "a trophy while learning much about the world.";
        String imageURL = GWT.getModuleBaseURL() + "images/trophies/trophy.png";
        //String imageURL = GWT.getModuleBaseURL() + "trophyimg?trophy=" + trophy.trophyId;
        String collectionURL = ctx.getEverythingURL(
            Kontagent.POST, tracking, Page.BROWSE, ctx.getMe().userId);
        String everyURL = ctx.getEverythingURL(Kontagent.POST, tracking, Page.LANDING);
        String everyURLText = "Start your own Everything collection!";
        String prompt = "Celebrate your trophy:";

        showDialog(title, desc, trophy.name, trophy.description,
            imageURL, collectionURL, everyURL, everyURLText, prompt,
            new Command() { // complete callback
                public void execute () {
                    _everysvc.storyPosted(tracking, new AsyncCallback<Void>() {
                        public void onSuccess (Void result) { /* nada */ }
                        public void onFailure (Throwable cause) {
                            Console.log("Failed to report story post",
                                "tracking", tracking, cause);
                        }
                    });
                }
            },
            new Command() { // incomplete callback
                public void execute () {
                    // nada
                }
            });
    }

    protected static native void showDialog (
        String title, String descrip, String trophyName, String trophyQual,
        String imageURL, String collectionURL, String everyURL, String everyURLText,
        String prompt, Command onComplete, Command onIncomplete)
    /*-{
        var publish = {
            method: "stream.publish",
            attachment: {
                name: title,
                href: everyURL,
                description: descrip,
                media: [{ type: 'image',
                          src: imageURL,
                          href: collectionURL }],
                properties: {'Trophy': trophyName,
                             'Qualification': trophyQual,
                             'Join the fun': { text: everyURLText,
                                               href: everyURL }},
            },
            actions: [{ name: "Collect Everything",
                        link: everyURL }],
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
