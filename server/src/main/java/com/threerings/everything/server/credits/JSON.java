//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server.credits;

import com.samskivert.util.StringUtil;

public class JSON {

    /** A POJO that models the real-time update notification from FB. */
    public static class Notification extends JSONObject {
        public static class Entry extends JSONObject {
            public String id;
            public long time;
            public String[] changed_fields;
        }
        public String object; // will be "payments" in our case
        public Entry[] entry;
    }

    /** A POJO that models the info on a Facebook payment. */
    public static class PaymentInfo extends JSONObject {
        public static class User extends JSONObject {
            public String id;
            public String name;
        }

        public static class App extends JSONObject {
            public String name;
            public String namespace;
            public String id;
        }

        public static class Action extends JSONObject {
            public String type;
            public String status;
            public String currency;
            public String amount;
            public String time_created;
            public String time_updated;
        }

        public static class RefundableAmount extends JSONObject {
            public String currency;
            public String amount;
        }

        public static class Item extends JSONObject {
            public String type;
            public String product;
            public int quantity;
        }

        public String id;
        public User user;
        public App application;
        public Action[] actions;
        public RefundableAmount refundable_amount;
        public Item[] items;
        public String country;
        public String created_time;
        public float payout_foreign_exchange_rate;
    }

    protected static class JSONObject {
        @Override public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }
}
