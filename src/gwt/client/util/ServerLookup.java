//
// $Id$

package client.util;

import com.threerings.gwt.util.MessagesLookup;

/**
 * Provides dynamic trnaslation lookup for messages coming back from the server.
 */
@MessagesLookup.Lookup(using="client.util.ServerMessages")
public abstract class ServerLookup extends MessagesLookup
{
}
