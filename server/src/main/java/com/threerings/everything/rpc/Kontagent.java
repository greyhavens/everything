//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.rpc;

/**
 * Represents the various Kontagent message types.
 */
public enum Kontagent
{
    INVITE("ins"),
    INVITE_RESPONSE("inr"),
    NOTIFICATION("nts"),
    NOTIFICATION_RESPONSE("ntr"),
    NOTIFICATION_EMAIL("nes"),
    NOTIFICATION_EMAIL_RESPONSE("nei"),
    POST("pst"),
    POST_RESPONSE("psr"),
    APP_ADDED("apa"),
    APP_REMOVED("apr"),
    OTHER_RESPONSE("ucc"),
    PAGE_REQUEST("pgr"),
    USER_INFO("cpu"),
    GOAL_COUNTS("gci"),
    REVENUE_GET("mtu"),
    NOOP("noop"); // not part of Kontagent API, we use it internally

    /** The three letter REST API code for this message type. */
    public final String code;

    /**
     * Returns the Kontagent message type for the specified code or {@link #OTHER_RESPONSE}.
     */
    public static Kontagent fromCode (String code)
    {
        for (Kontagent type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return OTHER_RESPONSE;
    }

    Kontagent (String code) {
        this.code = code;
    }
}
