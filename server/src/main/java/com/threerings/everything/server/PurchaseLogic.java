//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import javax.xml.bind.DatatypeConverter;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.app.client.ServiceException;

import com.threerings.everything.rpc.EveryAPI;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.RedemptionRepository;

import static com.threerings.everything.Log.log;

@Singleton
public class PurchaseLogic
{
    @Inject public PurchaseLogic (EverythingApp app) {
        _app = app;
        _playStoreKey = decodePubKey(_app.getPlayStoreKey());
    }

    public int redeemPurchase (PlayerRecord player, String sku, String platform,
                               String token, String receipt) throws ServiceException {
        int uidx = sku.lastIndexOf("_");
        ServiceException.require(uidx != -1, "e.invalid_sku");
        int coins = Integer.parseInt(sku.substring(uidx+1));

        // make sure the receipt in question is valid before we do anything else
        if (EveryAPI.PF_PLAYSTORE.equals(platform)) validateGoogleReceipt(sku, receipt);
        else if (EveryAPI.PF_APPSTORE.equals(platform)) validateAppleReceipt(sku, receipt);
        else if (EveryAPI.PF_TEST.equals(platform)) validateTestReceipt(sku, receipt);
        else throw new ServiceException("e.unknown_platform");

        // if this token has already been redeemed, then ignore this request
        if (!_redeemRepo.noteRedemption(token, player.userId, platform)) {
            log.info("Ignoring repeated redemption.", "who", player.who(), "token", token,
                     "platform", platform);
            return player.coins;
        }

        // grant the appropriate number of coins to the player
        _app.coinsPurchased(player.userId, coins);
        return player.coins + coins;
    }

    protected void validateGoogleReceipt (String sku, String rcpt) throws ServiceException {
        String[] bits = rcpt.split("\n", 2);
        ServiceException.require(bits.length == 2, "e.invalid_receipt");
        String sig = bits[0], data = bits[1];
        if (data.startsWith("\n")) data = data.substring(1);
        try {
            Signature signer = Signature.getInstance("SHA1withRSA");
            signer.initVerify(_playStoreKey);
            signer.update(data.getBytes("UTF-8"));
            if (!signer.verify(_base64.decode(sig))) {
                log.warning("Rejecting invalid Google receipt", "sku", sku,
                            "rdata", data, "rsig", sig);
                throw new ServiceException("e.invalid_receipt");
            }
        } catch (Exception e) {
            log.warning("Failed to validate Google receipt", "sku", sku, "rcpt", rcpt, "error", e);
            throw new ServiceException("e.invalid_receipt");
        }
    }

    protected void validateAppleReceipt (String sku, String receipt) throws ServiceException {
        // send the receipt to the app store verification server
        try {
            String rcpt64 = DatatypeConverter.printBase64Binary(receipt.getBytes("UTF-8"));
            String payload = _gson.toJson(ImmutableMap.of("receipt-data", rcpt64));
            String url = _app.isCandidate() ? "https://sandbox.itunes.apple.com/verifyReceipt" :
                "https://buy.itunes.apple.com/verifyReceipt";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            try { // christ Java, really?
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setAllowUserInteraction(false);
                conn.setRequestProperty("Content-type", "text/json");
                conn.connect();
                conn.getOutputStream().write(payload.getBytes("UTF-8"));
                conn.getOutputStream().close();

                // read and decode the response
                int code = conn.getResponseCode();
                byte[] brsp = ByteStreams.toByteArray(
                    code >= 400 ? conn.getErrorStream() : conn.getInputStream());
                String encoding = conn.getContentEncoding();
                if (encoding == null) encoding = "UTF-8";
                String rsp = new String(brsp, encoding);

                if (code != 200) {
                    log.warning("Failed to validate AppStore receipt", "sku", sku, "rcpt", receipt,
                                "error", rsp);
                    throw new ServiceException("e.server_error");
                }

                // we got some reasonable response from the app store server, decode it
                class AppleResponse { int status; }
                AppleResponse arsp = _gson.fromJson(rsp, AppleResponse.class);
                if (arsp.status != 0) {
                    log.warning("Non-zero 'status' field in App Store verification server response",
                                "sku", sku, "rcpt", receipt, "rsp", rsp);
                    throw new ServiceException("e.invalid_receipt");
                }
                // otherwise we're good to go, just return from the function

            } finally {
                conn.disconnect();
            }

        } catch (Exception e) {
            log.warning("Failed to validate AppStore receipt", "sku", sku, "rcpt", receipt, e);
            throw new ServiceException("e.internal_error");
        }
    }

    protected void validateTestReceipt (String sku, String receipt) throws ServiceException {
        if (!_app.isCandidate()) throw new ServiceException("e.not_test_env");
        if (!receipt.equals("test_rcpt:" + sku)) throw new ServiceException("e.invalid_receipt");
        if ("coins_24000".equals(sku)) throw new ServiceException("e.server_error");
    }

    protected PublicKey decodePubKey (String key) {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(_base64.decode(key)));
        } catch (Exception e) {
            log.warning("Failed to process public key", "key", key, e);
            return null;
        }
    }

    protected final BaseEncoding _base64 = BaseEncoding.base64();
    protected final EverythingApp _app;
    protected final PublicKey _playStoreKey;

    protected final Gson _gson = new GsonBuilder().
        setLongSerializationPolicy(LongSerializationPolicy.STRING).
        create();

    @Inject protected RedemptionRepository _redeemRepo;
}
