//
// $Id$

package client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;

/**
 * Utility methods for dealing with XFBML.
 */
public class XFBML
{
    /**
     * Creates an XFBML tag with the supplied tag name and attribute name/value pairs. Neither the
     * tag, names or values are escaped. For example:
     *
     * <pre>
     * Widget photo = XFBML.newTag("photo", "pid", "12345");
     * // creates
     * <fb:photo pid="12345"></fb:photo>
     * </pre>
     *
     * Any panel that adds XFBML widgets to itself must parse them with a call to {@link #parse}
     * once they are attached to the DOM. Because this is done recursively, it is generally most
     * appropriate to have any top-level page that will contain XFBML elements call {@link #parse}
     * in its {@link Widget#onLoad} method to resolve all XFBML content in a single pass.
     */
    public static Widget newTag (String tag, String... attrsValues)
    {
        return new XFBMLWidget(tag, attrsValues);
    }

    /**
     * Creates a tag with {@link #newTag} and marks it as rendering inline.
     */
    public static Widget newInlineTag (String tag, String... attrsValues)
    {
        Widget xtag = newTag(tag, attrsValues);
        xtag.addStyleName("inline");
        return xtag;
    }

    /**
     * Creates an XFBML panel that uses the supplied tag name and attribute name/value pairs and
     * can contain other XFBML widgets.
     */
    public static FlowPanel newPanel (String tag, String... attrsValues)
    {
        return new XFBMLPanel(tag, attrsValues);
    }

    /**
     * Creates a hidden input form field to place inside a request-form tag to pass information to
     * the servlet that receives the request-form POST.
     */
    public static Widget newHiddenInput (final String name, final String value)
    {
        return new Widget() {
            /*constructor*/ {
                setElement(DOM.createElement("input"));
                getElement().setAttribute("fb_protected", "true");
                getElement().setAttribute("type", "hidden");
                getElement().setAttribute("name", name);
                getElement().setAttribute("value", value);
            }
        };
    }

    /**
     * Wraps the supplied widget in a fb:serverfbml div and sacrifices the necessary chickens to
     * make it work. The returned widget will automatically call {@link #parse} on itself when it
     * is added to the DOM.
     */
    public static Widget serverize (Widget target, String... attrsValues)
    {
        FlowPanel panel = new XFBMLPanel("serverfbml") {
            protected void onLoad () {
                super.onLoad();
                parse(getParent()); // we need to parse our parent for some reason
            }
        };
        addAttributes(panel.getElement(), attrsValues);
        Element script = DOM.createElement("script");
        script.setAttribute("type", "text/fbml");
        panel.getElement().appendChild(script);
        // now for the tricky bit:
        //   1. GWT and/or DOM does not support adding child nodes to a script tag
        //   2. IE does not support setting innerHTML on script tags (it just silently blanks it)
        //   3. IE crashes when setting innerText on script tags
        // so... write to the "text" property of the script tag. voila
        // http://www.codingforums.com/archive/index.php/t-39551.html
        String fbml = Widgets.newFlowPanel(target).getElement().getInnerHTML();
        script.setPropertyString("text", "<fb:fbml>" + fbml + "</fb:fbml>");
        return panel;
    }

    /**
     * Returns a profile picture for the specified user, which links to their profile.
     */
    public static Widget newProfilePic (long facebookId)
    {
        // TODO: wrap this in a link to their profile: http://www.facebook.com/people/fbid
        return newTag("profile-pic", "uid", ""+facebookId, "linked", "false");
    }

    /**
     * Creates a comments box with the specified unique id, title and number of visible posts.
     */
    public static Widget newCommentsBox (String xid, String title, int vizposts)
    {
        return newTag("comments", "xid", xid, "numposts", ""+vizposts, "title", title);
    }

    /**
     * Parses the XFBML widgets in the supplied DOM tree rooted at the specified element.
     */
    public static void parse (Widget root)
    {
        nativeParse(root.getElement());
    }

    protected static void addAttributes (Element element, String... attrsValues)
    {
        for (int ii = 0; ii < attrsValues.length; ii += 2) {
            element.setAttribute(attrsValues[ii], attrsValues[ii+1]);
        }
    }

    protected static class XFBMLWidget extends Widget
    {
        public XFBMLWidget (String tag, String... attrsValues) {
            setElement(DOM.createElement(NAMESPACE + tag));
            // we need to have <fb:x></fb:x>, not <fb:x/>, so add an empty text node
            getElement().appendChild(Document.get().createTextNode(""));
            addAttributes(getElement(), attrsValues);
        }
    }

    protected static class XFBMLPanel extends FlowPanel
    {
        public XFBMLPanel (String tag, String... attrsValues) {
            setElement(DOM.createElement(NAMESPACE + tag));
            addAttributes(getElement(), attrsValues);
        }
    }

    protected static native void nativeParse (Element elem) /*-{
        try {
            $wnd.FB.Bootstrap.ensureInit(function () {
                $wnd.FB.XFBML.Host.parseDomElement(elem);
            });
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.log("Failed to reparse XFBML [error=" + e + "]");
            }
        }
    }-*/;

    /** The namespace for XFBML tags. */
    protected static final String NAMESPACE = "fb:";
}
