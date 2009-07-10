//
// $Id$

package com.threerings.everything.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.samsara.app.client.ServiceException;

import com.threerings.everything.data.SessionData;

/**
 * Defines the services available to the Everything client.
 */
@RemoteServiceRelativePath(EverythingService.ENTRY_POINT)
public interface EverythingService extends RemoteService
{
    /** The path at which this servlet is mapped. */
    public static final String ENTRY_POINT = "everything";

    /**
     * Validates that this client has proper session credentials. Returns null if they do not.
     */
    SessionData validateSession (int tzOffset) throws ServiceException;
}
