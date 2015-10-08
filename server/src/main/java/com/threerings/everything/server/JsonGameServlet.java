//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server;

import java.io.IOException;

import com.google.inject.Inject;

import com.threerings.app.client.ServiceException;
import com.threerings.everything.data.CardIdent;
import static com.threerings.everything.rpc.JSON.*;

public class JsonGameServlet extends JsonServiceServlet {

    protected Object handle (String method)
        throws IOException, ServiceException
    {
        if ("/getCollection".equals(method)) {
            GetCollection args = readArgs(GetCollection.class);
            return _collLogic.getCollection(args.ownerId);

        } else if ("/getSeries".equals(method)) {
            GetSeries args = readArgs(GetSeries.class);
            return _collLogic.getSeries(args.ownerId, args.categoryId);

        } else if ("/getCard".equals(method)) {
            CardIdent ident = readArgs(CardIdent.class);
            return _cardLogic.getCard(ident);

        } else if ("/getGrid".equals(method)) {
            GetGrid args = readArgs(GetGrid.class);
            return _gameLogic.getGrid(requirePlayer(), args.pup, args.expectHave);

        } else if ("/flipCard".equals(method)) {
            FlipCard args = readArgs(FlipCard.class);
            return _cardLogic.flipCard(requirePlayer(), args.gridId, args.pos, args.expectCost);

        } else if ("/sellCard".equals(method)) {
            CardInfo args = readArgs(CardInfo.class);
            return _cardLogic.sellCard(requirePlayer(), args.thingId, args.created);

        } else if ("/getGiftCardInfo".equals(method)) {
            CardInfo args = readArgs(CardInfo.class);
            return _cardLogic.getGiftCardInfo(requirePlayer(), args.thingId, args.created);

        } else if ("/giftCard".equals(method)) {
            GiftCard args = readArgs(GiftCard.class);
            _cardLogic.giftCard(requirePlayer(), args.thingId, args.created,
                                args.friendId, args.message);
            return "ok";

        } else if ("/setLike".equals(method)) {
            SetLike args = readArgs(SetLike.class);
            _collLogic.setLike(requirePlayer(), args.catId, args.like);
            return "ok";

        } else if ("/openGift".equals(method)) {
            CardInfo args = readArgs(CardInfo.class);
            return _cardLogic.openGift(requirePlayer(), args.thingId, args.created);

        } else if ("/getShopInfo".equals(method)) {
            return _gameLogic.getShopInfo(requirePlayer());

        } else if ("/buyPowerup".equals(method)) {
            BuyPowerup args = readArgs(BuyPowerup.class);
            _gameLogic.buyPowerup(requirePlayer(), args.pup);
            return "ok";

        } else if ("/usePowerup".equals(method)) {
            UsePowerup args = readArgs(UsePowerup.class);
            return _gameLogic.usePowerup(requirePlayer(), args.gridId, args.pup);

        } else {
            return null;
        }
    }

    @Inject protected GameLogic _gameLogic;
    @Inject protected CardLogic _cardLogic;
    @Inject protected ThingLogic _thingLogic;
    @Inject protected CollectionLogic _collLogic;
}
