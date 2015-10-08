//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server.credits;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.everything.data.CoinPrices;
import com.threerings.everything.server.EverythingApp;

@Singleton
public class ProductServlet extends HttpServlet {

    @Override
    public void doGet (HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        // look up the product in question
        String path = req.getPathInfo();
        CoinPrices.Offer offer;
        try {
            offer = CoinPrices.getOffer(Integer.parseInt(path.substring(1)));
        } catch (Exception e) {
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid offer: " + path);
            return;
        }

        StringBuilder buf = new StringBuilder();
        buf.append(HEAD_OPEN);
        appendMeta(buf, "og:type", "og:product");
        appendMeta(buf, "og:title", offer.coins + " Everything Coins");
        appendMeta(buf, "og:plural_title", offer.coins + " Everything Coins");
        appendMeta(buf, "og:image", "https://everything.herokuapp.com/images/money50.png");
        appendMeta(buf, "og:description", "Flip all your cards and get powerups with more coins!");
        appendMeta(buf, "og:url", _app.getBackendURL() + CoinPrices.OG_PATH + "/" + offer.id);
        appendMeta(buf, "og:price:amount", (offer.pennies / 100) + "." + (offer.pennies % 100));
        appendMeta(buf, "og:price:currency", "USD");
        buf.append(HEAD_CLOSE);

        rsp.setContentType("text/html");
        rsp.getWriter().print(buf);
    }

    protected void appendMeta (StringBuilder buf, String prop, String content) {
        buf.append("<meta property=\"").append(prop).append("\" ").
            append("content=\"").append(content).append("\" />\n");
    }

    @Inject protected EverythingApp _app;

    protected static final String HEAD_OPEN =
        "<head prefix=\"og: http://ogp.me/ns# fb: http://ogp.me/ns/fb# " +
        "product: http://ogp.me/ns/product#\">\n";
    protected static final String HEAD_CLOSE = "</head>\n";
}
