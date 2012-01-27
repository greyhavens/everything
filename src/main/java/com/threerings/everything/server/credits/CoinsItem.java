//
// $Id$

package com.threerings.everything.server.credits;

import com.google.gson.Gson;

import com.threerings.everything.data.CoinPrices;
import com.threerings.everything.server.EverythingApp;

public class CoinsItem
{
    public String item_id;
    public String title;
    public String description =
        "Coins can be used to flip cards and purchase powerups in Everything.";
    public String image_url;
    public int price;
    public int data;

    public void init (EverythingApp app, int offerId)
    {
        CoinPrices.Offer offer = CoinPrices.getOffer(offerId);
        item_id = ""+offerId;
        title = offer.coins + " Everything Coins";
        price = offer.credits;
        image_url = app.getBaseUrl() + "images/money.png";
        data = offerId;
    }
}
