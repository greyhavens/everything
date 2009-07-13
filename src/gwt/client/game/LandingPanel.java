//
// $Id$

package client.game;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;

import client.util.Context;

/**
 * The main interface displayed when the player arrives at the game.
 */
public class LandingPanel extends FlowPanel
{
    public LandingPanel (Context ctx)
    {
        setStyleName("landing");

        // TODO: nix help once they've had 3 sessions
        add(Widgets.newLabel("Welcome to Everything!", "Title"));
        FlowPanel intro = new FlowPanel();
        for (String text : INTRO_HTML) {
            intro.add(Widgets.newHTML(text, "Text"));
        }
        add(intro);

        add(Widgets.newLabel("Latest News", "Title"));
        add(Widgets.newHTML(NEWS, "Text")); // TODO: load from DB

        add(Widgets.newLabel("Recent Happenings", "Title"));
        add(Widgets.newHTML("Coming soon!", "Text"));
    }

    protected static final String[] INTRO_HTML = {
        "Everything is a collecting game. Every day you get a new grid of cards and you " +
        "flip them over to get new things to add to your collection. Some cards are rarer " +
        "than others, so good luck.",
        "You get three free flips every day, but you can buy more flips by spending coins " +
        "you get from cashing in cards you don't want. Rarer cards are worth more coins when " +
        "you cash them in. You can also give cards to your friends to help them complete their " +
        "collections.",
        "Enough jabber, click <b>Flip Cards</b> up above to get started!"
    };

    protected static final String NEWS = "We're just getting started. The user interface is " +
        "about as programmery as it gets and our giant database of everything in the entire " +
        "universe is still pretty small. But there's some awesome stuff in there. " +
        "These are the new features we'll be adding soon:" +
        "<ul><li> A feed of what cool stuff your friends have been flipping and giving to each other. </li>" +
        "<li> A way to browse your friends' collections. </li>" +
        "<li> A wishlist so that you can let your friends know what you're looking for. </li>" +
        "<li> A prettier user interface! </li>" +
        "<li> Lots more things in the database. </li></ul>";
}
