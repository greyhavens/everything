//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server.credits;

/**
 * A JSON record sent to Facebook responding to a notification that the status of a purchase has
 * changed.
 */
public class StatusUpdate extends CreditsResponse
{
    public static final String METHOD_NAME = "payments_status_update";

    public Status content;

    public StatusUpdate (String status)
    {
        super(METHOD_NAME);
        content = new Status(status);
    }

    protected static class Status {
        public final String status;

        public Status (String status) {
            this.status = status;
        }
    }
}
