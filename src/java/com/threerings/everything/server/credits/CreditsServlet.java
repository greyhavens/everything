//
// $Id$

package com.threerings.everything.server.credits;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.samskivert.servlet.HttpErrorException;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.samsara.common.UserLogic;
import com.threerings.servlet.util.Parameters;

import com.threerings.everything.data.CoinPrices;
import com.threerings.everything.server.EverythingApp;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;

import static com.threerings.everything.Log.log;

/**
 * Handles the interactions with the Facebook Credits backend.
 * See http://developers.facebook.com/docs/creditsapi/
 */
@Singleton
public class CreditsServlet extends HttpServlet
{
    @Inject public CreditsServlet (ProviderCreator<OrderDetails> odCreator,
                                   ProviderCreator<CoinsItem> ciCreator)
    {
        _coiner = ciCreator.getProvider();
        _gson = new GsonBuilder().
            registerTypeAdapter(OrderDetails.class, odCreator).
            registerTypeAdapter(CoinsItem.class, ciCreator).
            excludeFieldsWithModifiers(Modifier.PROTECTED, Modifier.STATIC).
            create();
    }

    @Override
    public void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        log.info("CreditsServlet.doPost", "params", getPrettyParams(req));

        // Check the fb_sig parameter to make sure this request came from Facebook's servers.
        if (!validateFacebookSig(req)) {
            log.warning("Request did not originate with Facebook", "params", getPrettyParams(req));
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        try {
            CreditsResponse response = processRequest(req, req.getParameter(METHOD));
            PrintWriter out = rsp.getWriter();
            out.print(_gson.toJson(response));
            out.close();
        } catch (HttpErrorException he) {
            rsp.sendError(he.getErrorCode());
        }
    }

    protected CreditsResponse processRequest (HttpServletRequest req, String method)
        throws HttpErrorException
    {
        if (ItemDescription.METHOD_NAME.equals(method)) {
            try {
                CoinsItem item = _coiner.get();
                item.init(_gson.fromJson(req.getParameter(ORDER_INFO), Integer.class));
                return new ItemDescription(item);
            } catch (Exception e) {
                log.warning("Failed to process item description request",
                            "params", getPrettyParams(req), "error", e);
                throw new HttpErrorException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } else if (StatusUpdate.METHOD_NAME.equals(method)) {
            return statusUpdate(req);

        } else {
            log.warning("Received Facebook Credits request with unknown method", "method", method,
                        "params", getPrettyParams(req));
            throw new HttpErrorException(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected CreditsResponse statusUpdate (HttpServletRequest req)
        throws HttpErrorException
    {
        OrderDetails details = _gson.fromJson(req.getParameter(ORDER_DETAILS), OrderDetails.class);
        if (PLACED.equals(details.status)) {
            // let's make sure we recognize this player, and cancel the order if we don't
            Map<String, Integer> userIds = _userLogic.mapFacebookIds(
                Collections.singletonList(details.receiver));
            if (!userIds.containsKey(details.receiver)) {
                log.warning("CreditsServlet receiver not recognized!", "fbId", details.receiver);
                return new StatusUpdate(CANCELED);
            } else {
                return new StatusUpdate(SETTLED);
            }

        } else if (SETTLED.equals(details.status)) {
            Map<String, Integer> userIds = _userLogic.mapFacebookIds(
                Collections.singletonList(details.receiver));
            Integer userId = userIds.get(details.receiver);
            if (userId == null) {
                log.warning("Settled payment has bad reciever id!", "fbId", details.receiver,
                            "orderId", details.order_id);
                return null;
            }

            // log the transaction with facebook
            log.info("Order settled", "id", details.order_id, "buyer", details.buyer,
                     "receiver", details.receiver, "item", details.items[0].item_id,
                     "data", details.items[0].data);
            // TODO: record this somewhere?
            // _playerRepo.recordFacebookCreditTransaction(
            //     details.order_id, details.buyer, details.receiver, details.items[0].item_id,
            //     details.items[0].data);

            // give the user the coins
            int offerId = details.items[0].data;
            try {
                CoinPrices.Offer offer = CoinPrices.getOffer(offerId);
                _app.coinsPurchased(userId, offer.coins);
                // FB doesn't use the response to a SETTLED call, but it needs the response to
                // contain the bare minimum, or it thinks we're dead and sends the request 3 times
                return new CreditsResponse(StatusUpdate.METHOD_NAME);

            } catch (Exception e) {
                log.warning("Requested to redeem invalid offer id", "offerId", offerId,
                            "params", getPrettyParams(req), e);
                throw new HttpErrorException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } else {
            log.warning("Received unexpected status update", "status", details.status,
                        "params", getPrettyParams(req));
            return null;
        }
    }

    protected boolean validateFacebookSig (HttpServletRequest req)
    {
        Parameters params = new Parameters(req);
        Map<String, String> sigParams = Maps.newTreeMap();
        for (Tuple<String, String> param : params.entries()) {
            if (param.left.startsWith(SIG_PARAMS_PREFIX)) {
                sigParams.put(param.left.substring(SIG_PARAMS_PREFIX.length()), param.right);
            }
        }
        StringBuilder keyString = new StringBuilder();
        for (Entry<String, String> entry : sigParams.entrySet()) {
            keyString.append(entry.getKey()).append("=").append(entry.getValue());
        }
        keyString.append(_app.getFacebookSecret());
        return StringUtil.md5hex(keyString.toString()).equals(params.get(FB_SIG));
    }

    protected String getPrettyParams (HttpServletRequest req)
    {
        return StringUtil.toString(Collections2.transform(new Parameters(req).entries(),
            // transform the tuples to avoid incredibly super verbose toString()s
            new Function<Tuple<String, String>, String>() {
                @Override public String apply (Tuple<String, String> from) {
                    return from.left + "=" + from.right;
                }
            }));
    }

    protected Provider<CoinsItem> _coiner;
    protected Gson _gson;

    @Inject protected EverythingApp _app;
    @Inject protected UserLogic _userLogic;
    @Inject protected PlayerRepository _playerRepo;

    // Parameters we're specifically interested in
    protected static final String METHOD = "method";
    protected static final String ORDER_INFO = "fb_sig_order_info";
    protected static final String SIG_PARAMS_PREFIX = "fb_sig_";
    protected static final String FB_SIG = "fb_sig";
    protected static final String ORDER_DETAILS = "fb_sig_order_details";

    // Statuses
    protected static final String PLACED = "placed";
    protected static final String SETTLED = "settled";
    protected static final String CANCELED = "canceled";
}
