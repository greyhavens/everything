//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.server.credits;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.app.server.UserLogic;
import com.threerings.user.ExternalAuther;

import com.threerings.everything.data.CoinPrices;
import com.threerings.everything.server.EverythingApp;

import static com.threerings.everything.Log.log;
import static com.threerings.everything.server.credits.JSON.*;

/**
 * A servlet that receives "real-time updates" on the payments object. This is Facebook Graph speak
 * for notifications during the Facebook payment process.
 */
@Singleton
public class PayUpServlet extends HttpServlet {

    @Inject public PayUpServlet () {
        _gson = new GsonBuilder().
            excludeFieldsWithModifiers(Modifier.PROTECTED, Modifier.STATIC).
            create();
    }

    @Override
    public void doPost (HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        Notification note = _gson.fromJson(req.getReader(), Notification.class);

        log.info("PayUp: " + note); // DEBUG

        if (!"payments".equals(note.object)) {
            log.warning("Got notification from non-'payments' object?", "note", note);
            return;
        }

        // go through each payment in this note (probably just one), and fetch its status
        for (Notification.Entry entry : note.entry) {
            try {
                String url = "https://graph.facebook.com/" + entry.id +
                    "?access_token=" + _app.getFacebookAppToken();
                URLConnection conn = new URL(url).openConnection();
                Reader in = new InputStreamReader(conn.getInputStream(), Charsets.UTF_8);
                PaymentInfo info = _gson.fromJson(new BufferedReader(in), PaymentInfo.class);
                in.close();

                // if this payment info contains a completed notification, award currency
                if (isCompleted(info)) {
                    String userId = info.user.id;
                    for (PaymentInfo.Item item : info.items) {
                        handlePurchase(info, userId, item.product);
                    }
                }

            } catch (Exception e) {
                log.warning("Failure processing entry", "entry", entry, e);
            }
        }
    }

    // TODO: this is all somewhat fragile now that there's no confirmation process; thanks FB!
    protected void handlePurchase (PaymentInfo info, String fbUserId, String productURL) {
        Map<String, Integer> userIds = _userLogic.mapExtAuthIds(
            ExternalAuther.FACEBOOK, Collections.singletonList(fbUserId));
        Integer userId = userIds.get(fbUserId);
        if (userId == null) {
            log.warning("Completed payment for unknown user!", "fbId", fbUserId, "info", info);
            return;
        }

        // log the transaction with facebook
        log.info("Order completed", "id", info.id, "item", productURL,
                 "fbId", fbUserId, "userId", userId);
        // TODO: record this somewhere?
        // _playerRepo.recordFacebookCreditTransaction(
        //     details.order_id, details.buyer, details.receiver, details.items[0].item_id,
        //     details.items[0].data);

        // determine which offer they bought and award the currency
        CoinPrices.Offer offer = CoinPrices.getOffer(_app.getBackendURL(), productURL);
        _app.coinsPurchased(userId, offer.coins);
    }

    protected boolean isCompleted (PaymentInfo info) {
        for (PaymentInfo.Action action : info.actions) {
            if ("completed".equals(action.status)) return true;
        }
        return false;
    }

    protected Gson _gson;

    @Inject protected EverythingApp _app;
    // @Inject protected FacebookConfig _fbconf;
    @Inject protected UserLogic _userLogic;
    // @Inject protected PlayerRepository _playerRepo;
}
