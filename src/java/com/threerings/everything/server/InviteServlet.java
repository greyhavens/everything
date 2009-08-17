//
// $Id$

package com.threerings.everything.server;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.StringUtil;
import com.threerings.user.OOOUser;

import com.threerings.samsara.app.server.AppServlet;
import com.threerings.samsara.app.server.UserLogic;

import com.threerings.everything.client.Kontagent;
import com.threerings.everything.data.Thing;
import com.threerings.everything.server.persist.CardRecord;
import com.threerings.everything.server.persist.GameRepository;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;
import com.threerings.everything.server.persist.ThingRepository;

import static com.threerings.everything.Log.log;

/**
 * Handles notification that we invited someone to play Everything.
 */
public class InviteServlet extends AppServlet
{
    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        // this path should only happen when they cancel the multi-friend-selector
        if (!StringUtil.isBlank(req.getParameter("ids[]"))) {
            log.warning("Got a GET request on invite servlet with ids",
                        "ids", req.getParameter("ids[]"));
        }
        writeClose(rsp, false);
    }

    @Override // from HttpServlet
    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        OOOUser user = getUser(req);
        PlayerRecord player = (user == null) ? null : _playerRepo.loadPlayer(user.userId);
        Set<String> targetFBIds = null;

        try {
            if (player == null) {
                throw new Exception("missing inviter");
            }

            targetFBIds = ParameterUtil.getParameters(req, "ids[]");
            if (targetFBIds.size() == 0) { // they chose to skip
                log.info("No targets, skipping", "who", player.who());
                writeClose(rsp, true);
                return;
            }

            int thingId = Integer.parseInt(ParameterUtil.getParameter(req, "thing", "0"));
            if (thingId > 0) {
                processThingGift(req, player, targetFBIds.iterator().next(), thingId);
            }

            // report to kontagent that we sent one or more invites
            String tracking = req.getParameter("tracking");
            if (StringUtil.isBlank(tracking)) {
                log.warning("Missing Kontagent tracking id for invitation.", "who", player.who(),
                            "target", targetFBIds);
            } else {
                _kontLogic.reportAction(Kontagent.INVITE, "s", player.facebookId,
                                        "r", Joiner.on(",").join(targetFBIds), "u", tracking);
            }

        } catch (Exception e) {
            log.warning("Failed to process invite gift: " + e.getMessage(),
                        "who", (player == null) ? "null" : player.who(),
                        "agent", req.getHeader("User-agent"), "targetFBIds", targetFBIds);
        }

        writeClose(rsp, true);
    }

    protected void processThingGift (HttpServletRequest req, PlayerRecord player, String targetFBId,
                                     int thingId)
        throws Exception
    {
        String received = requireParameter(req, "received");

        // make sure they own the thing in question
        CardRecord card = _gameRepo.loadCard(player.userId, thingId, Long.parseLong(received));
        if (card == null) {
            throw new Exception("missing card");
        }
        Thing thing = _thingRepo.loadThing(card.thingId);
        if (thing == null) {
            throw new Exception("missing thing");
        }

        // see if the recipient in question is already a player
        Map<String, Integer> ids = _userLogic.mapFacebookIds(Collections.singletonList(targetFBId));
        Integer targetId = ids.get(targetFBId);
        PlayerRecord target;
        if (targetId != null && (target = _playerRepo.loadPlayer(targetId)) != null) {
            log.info("Gifting card directly to player", "gifter", player.who(),
                     "thing", card.thingId, "recip", target.who());
            _gameLogic.giftCard(player, card, target, null);

        } else {
            log.info("Escrowing card for hopeful future player", "gifter", player.who(),
                     "thing", card.thingId, "recip", targetFBId);
            _gameRepo.escrowCard(card, targetFBId);

            // send them a Facebook notification as well as an invite
            _playerLogic.sendGiftNotification(
                player, Long.parseLong(targetFBId), thing, null, false, null);
        }
    }

    protected String requireParameter (HttpServletRequest req, String name)
        throws Exception
    {
        String value = ParameterUtil.getParameter(req, name, false);
        if (StringUtil.isBlank(value)) {
            throw new Exception("missing param '" + name + "'");
        }
        return value;
    }

    protected static void writeClose (HttpServletResponse rsp, boolean completed)
        throws IOException
    {
        // we're in an iframe so we have to send down some JavaScript that jimmies
        PrintWriter out = new PrintWriter(new OutputStreamWriter(rsp.getOutputStream()));
        out.println("<html><head><script language=\"JavaScript\">");
        out.println("window.parent.closePopup(" + completed + ");");
        out.println("</script></head></html>");
        out.close();
    }

    @Inject protected EverythingApp _app;
    @Inject protected GameLogic _gameLogic;
    @Inject protected GameRepository _gameRepo;
    @Inject protected KontagentLogic _kontLogic;
    @Inject protected PlayerLogic _playerLogic;
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected ThingRepository _thingRepo;
    @Inject protected UserLogic _userLogic;
}
