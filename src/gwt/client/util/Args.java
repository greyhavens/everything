//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.Hyperlink;

import com.threerings.everything.data.PlayerName;

/**
 * Handles parsing our page arguments.
 */
public class Args
{
    /** The page represented by this parsed args. */
    public final Page page;

    /**
     * Creates a link to the specified page with the specified label text.
     */
    public static Hyperlink createLink (String label, Page page, Object... args)
    {
        return createLink(label, null, page, args);
    }

    /**
     * Creates an inline link to the specified page with the specified label text.
     */
    public static Hyperlink createInlink (String label, Page page, Object... args)
    {
        return createLink(label, "inline", page, args);
    }

    /**
     * Creates a link to the specified page with the specified label text and additional style.
     */
    public static Hyperlink createLink (String label, String styleName, Page page, Object... args)
    {
        StringBuffer buf = new StringBuffer();
        buf.append(page);
        for (Object arg : args) {
            buf.append(SEPARATOR);
            buf.append(arg);
        }
        Hyperlink link = new Hyperlink(label, buf.toString());
        if (styleName != null) {
            link.addStyleName(styleName);
        }
        return link;
    }

    /**
     * Creates a link to the specified player's collection.
     */
    public static Hyperlink createInlink (PlayerName name)
    {
        return createInlink(name.toString(), Page.BROWSE, name.userId);
    }

    /**
     * Parses the supplied history token into an args instance.
     */
    public Args (String token)
    {
        String[] args = token.split(SEPARATOR);
        if (args.length > 0) {
            this.page = parsePage(args[0]);
            _args = new String[args.length-1];
            System.arraycopy(args, 1, _args, 0, _args.length);
        } else {
            this.page = Page.LANDING;
            _args = new String[0];
        }
    }

    /**
     * Returns the argument at the specified index or the default value if an argument was not
     * specified at that index.
     */
    public String get (int index, String defval)
    {
        return (index < _args.length) ? _args[index] : defval;
    }

    /**
     * Returns the argument at the specified index or the default value if no argument or a
     * non-integer argument was provided at that index.
     */
    public int get (int index, int defval)
    {
        try {
            return (index < _args.length) ? Integer.parseInt(_args[index]) : defval;
        } catch (Exception e) {
            return defval;
        }
    }

    protected static Page parsePage (String pagestr)
    {
        try {
            return Enum.valueOf(Page.class, pagestr);
        } catch (Exception e) {
            return Page.LANDING;
        }
    }

    protected String[] _args;

    protected static final String SEPARATOR = "~";
}
