//
// $Id$

package client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;

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
     * Parses the XFBML widgets in the supplied DOM tree rooted at the specified element.
     */
    public static void parse (Element root)
    {
        nativeParse(root);
    }

    protected static class XFBMLWidget extends Widget
    {
        public XFBMLWidget (String tag, String... attrsValues)
        {
            setElement(DOM.createElement(NAMESPACE + tag));
            // we need to have <fb:x></fb:x>, not <fb:x/>, so add an empty text node
            getElement().appendChild(Document.get().createTextNode(""));
            for (int ii = 0; ii < attrsValues.length; ii += 2) {
                getElement().setAttribute(attrsValues[ii], attrsValues[ii+1]);
            }
        }
    }

    protected static native void nativeParse (Element root) /*-{
        try {
            $wnd.FB_ParseXFBML(root);
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.log("Failed to reparse XFBML [error=" + e + "]");
            }
        }
    }-*/;

    /** The namespace for XFBML tags. */
    protected static final String NAMESPACE = "fb:";
}
