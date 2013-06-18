//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.server;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.threerings.app.client.ServiceException;
import com.threerings.everything.data.CardIdent;
import static com.threerings.everything.rpc.JSON.*;

public class JsonGameServlet extends JsonServiceServlet {

    protected Object handle (String method, HttpServletRequest req, HttpServletResponse rsp)
        throws IOException, ServiceException
    {
        if ("/getCollection".equals(method)) {
            GetCollection args = readArgs(req, GetCollection.class);
            return _collLogic.getCollection(args.ownerId);

        } else if ("/getSeries".equals(method)) {
            GetSeries args = readArgs(req, GetSeries.class);
            return _collLogic.getSeries(args.ownerId, args.categoryId);

        } else if ("/getCard".equals(method)) {
            CardIdent ident = readArgs(req, CardIdent.class);
            return _cardLogic.getCard(ident);

        } else if ("/getGrid".equals(method)) {
            GetGrid args = readArgs(req, GetGrid.class);
            return _gameLogic.getGrid(requirePlayer(req), args.pup, args.expectHave);

        } else if ("/flipCard".equals(method)) {
            FlipCard args = readArgs(req, FlipCard.class);
            return _cardLogic.flipCard(requirePlayer(req), args.gridId, args.pos, args.expectCost);

        } else if ("/sellCard".equals(method)) {
            CardInfo args = readArgs(req, CardInfo.class);
            return _cardLogic.sellCard(requirePlayer(req), args.thingId, args.created);

        } else if ("/getGiftCardInfo".equals(method)) {
            CardInfo args = readArgs(req, CardInfo.class);
            return _cardLogic.getGiftCardInfo(requirePlayer(req), args.thingId, args.created);

        } else if ("/giftCard".equals(method)) {
            GiftCard args = readArgs(req, GiftCard.class);
            _cardLogic.giftCard(requirePlayer(req), args.thingId, args.created,
                                args.friendId, args.message);
            return "ok";

        } else if ("/setLike".equals(method)) {
            SetLike args = readArgs(req, SetLike.class);
            _collLogic.setLike(requirePlayer(req), args.catId, args.like);
            return "ok";

        } else if ("/openGift".equals(method)) {
            CardInfo args = readArgs(req, CardInfo.class);
            return _cardLogic.openGift(requirePlayer(req), args.thingId, args.created);

        } else if ("/getShopInfo".equals(method)) {
            return _gameLogic.getShopInfo(requirePlayer(req));

        } else if ("/buyPowerup".equals(method)) {
            BuyPowerup args = readArgs(req, BuyPowerup.class);
            _gameLogic.buyPowerup(requirePlayer(req), args.pup);
            return "ok";

        } else if ("/usePowerup".equals(method)) {
            UsePowerup args = readArgs(req, UsePowerup.class);
            return _gameLogic.usePowerup(requirePlayer(req), args.gridId, args.pup);

        } else {
            return null;
        }
    }

    @Inject protected GameLogic _gameLogic;
    @Inject protected CardLogic _cardLogic;
    @Inject protected ThingLogic _thingLogic;
    @Inject protected CollectionLogic _collLogic;
}
