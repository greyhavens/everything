//
// $Id$

package com.threerings.everything.server.credits;

/**
 * A JSON record sent by Facebook providing details on an in-progress order.
 */
public class OrderDetails
{
    public long order_id;
    public long buyer;
    public String app;
    public String receiver;
    public int amount;
    public String update_time;
    public String time_placed;
    public String data;
    public CoinsItem[] items;
    public String status;
}
