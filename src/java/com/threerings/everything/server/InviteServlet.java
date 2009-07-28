//
// $Id$

package com.threerings.everything.server;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

import com.samskivert.servlet.util.ParameterUtil;
import com.samskivert.util.StringUtil;
import com.threerings.user.OOOUser;

import com.threerings.samsara.app.server.AppServlet;
import com.threerings.samsara.app.server.UserLogic;

import com.threerings.everything.server.persist.CardRecord;
import com.threerings.everything.server.persist.GameRepository;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;

import static com.threerings.everything.Log.log;

/**
 * Handles notification that we invited someone to play Everything.
 */
public class InviteServlet extends AppServlet
{
    @Override // from HttpServlet
    protected void doPost (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        OOOUser user = getUser(req);
        PlayerRecord player = (user == null) ? null : _playerRepo.loadPlayer(user.userId);
        String from = ParameterUtil.getParameter(req, "from", "LANDING");
        String targetFBId = null, thingId = null, created = null;

        try {
            if (player == null) {
                throw new Exception("missing inviter");
            }

            targetFBId = requireParameter(req, "friend_selector_id");
            thingId = requireParameter(req, "thing");
            created = requireParameter(req, "created");

            // make sure they own the thing in question
            CardRecord card = _gameRepo.loadCard(
                player.userId, Integer.parseInt(thingId), Long.parseLong(created));
            if (card == null) {
                throw new Exception("missing card");
            }

            // see if the recipient in question is already a player
            Map<String, Integer> ids = _userLogic.mapFacebookIds(
                Collections.singletonList(targetFBId));
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
            }

        } catch (Exception e) {
            log.warning("Failed to process invite gift: " + e.getMessage(),
                        "who", (player == null) ? "null" : player.who(),
                        "agent", req.getHeader("User-agent"), "targetFBId", targetFBId,
                        "thingId", thingId, "created", created);
        }

        // one way or the other, send them back from whence they came
        writeFrameRedirect(rsp, _app.getFacebookAppURL(from));
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

    @Inject protected EverythingApp _app;
    @Inject protected GameLogic _gameLogic;
    @Inject protected GameRepository _gameRepo;
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected UserLogic _userLogic;
}
