//
// $Id$

package client.ui;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.HTML;

import com.threerings.gwt.ui.WidgetUtil;

/**
 * Used to build Flash embed tags.
 */
public class FlashBuilder
{
    public final String id;

    public FlashBuilder (String id, String type)
    {
        this.id = id;
        _args.append("id=").append(id).append("&type=").append(type);
    }

    public void addOr (String key, Object value, String deftext)
    {
        add(key, (value == null) ? deftext : value.toString());
    }

    public void addIf (String key, Object value)
    {
        if (value != null) {
            add(key, value.toString());
        }
    }

    public void add (String key, String value)
    {
        _args.append("&").append(key).append("=").append(URL.encodeComponent(value));
    }

    public HTML build (int width, int height, boolean transparent)
    {
        return transparent ?
            WidgetUtil.createTransparentFlashContainer(id, SWF, width, height, _args.toString()) :
            WidgetUtil.createFlashContainer(id, SWF, width, height, _args.toString());
    }

    protected StringBuilder _args = new StringBuilder();

    protected static final String SWF = "everything-ui.swf";
}
