//
// $Id$

package com.threerings.everything.server;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.samskivert.io.StreamUtil;
import com.samskivert.servlet.util.ParameterUtil;
import com.threerings.user.OOOUser;

import com.threerings.samsara.app.data.AppCodes;
import com.threerings.samsara.app.server.AppServlet;

import com.threerings.everything.client.Kontagent;
import com.threerings.everything.data.Thing;
import com.threerings.everything.server.persist.CardRecord;
import com.threerings.everything.server.persist.GameRepository;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.everything.server.persist.PlayerRepository;
import com.threerings.everything.server.persist.RecruitGiftRecord;
import com.threerings.everything.server.persist.ThingRepository;

import static com.threerings.everything.Log.log;

/**
 * Spits out the XFBML necessary to display a Facebook invite dialog.
 */
public class ShowInviteServlet extends AppServlet
{
    @Inject public ShowInviteServlet (@Named(AppCodes.APPROOT) File approot)
    {
        File tmpl = new File(new File(approot, "web"), "show_invite.html");
        try {
            _template = StreamUtil.toString(new FileReader(tmpl));
        } catch (IOException ioe) {
            log.warning("Failed to read template", "tmpl", tmpl, ioe);
            _template = "Oh noez! We're broke.";
        }
    }

    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        OOOUser user = getUser(req);
        PlayerRecord player = (user == null) ? null : _playerRepo.loadPlayer(user.userId);
        if (player == null) {
            log.warning("Got show invite request but have no player?", "user", user);
            InviteServlet.writeClose(rsp, false);
            return;
        }

        Map<String, String> subs = Maps.newHashMap();
        subs.put("ACTION_URL", req.getRequestURL().toString().replaceAll("showinvite", "invite"));

        String tracking = _kontLogic.generateUniqueId(player.userId);
        subs.put("URL", _app.getHelloURL(Kontagent.INVITE, tracking));
        subs.put("TRACKING", tracking);
        subs.put("FACEBOOK_KEY", _app.getFacebookKey());

        // if they specified a card, load that up
        CardRecord card = null;
        int thingId = Integer.parseInt(ParameterUtil.getParameter(req, "thing", "0"));
        long received = Long.parseLong(ParameterUtil.getParameter(req, "received", "0"));
        if (player != null && thingId > 0) {
            if (received > 0L) {
                card = _gameRepo.loadCard(player.userId, thingId, received);

            } else if (received == 0L) {
                // this should match their recruitment gift..
                RecruitGiftRecord recruit = _playerRepo.loadRecruitGift(player.userId);
                if (recruit != null && (recruit.giftId == thingId)) {
                    // fake up a CardRecord
                    card = new CardRecord();
                    card.thingId = thingId;
                }
            }
        }

        if (card == null) {
            subs.put("ACTION", "Who do you want to invite to play The Everything Game?");
            subs.put("TITLE", player.name + " wants you to play The Everything Game with them.");
            subs.put("BUTTON", "Play Everything");
            subs.put("MAX_INVITES", "4");
            subs.put("THING_ID", "0");
            subs.put("RECEIVED", "0");

        } else {
            Thing thing = _thingRepo.loadThing(card.thingId);
            subs.put("ACTION", "Who do you want to give the " + thing.name + " card to?");
            subs.put("TITLE", player.name + " wants you to have the <b>" + thing.name +
                     "</b> card in The Everything Game.");
            subs.put("BUTTON", "View the card!");
            subs.put("MAX_INVITES", "1");
            subs.put("THING_ID", ""+card.thingId);
            subs.put("RECEIVED", "" + ((card.received == null) ? 0L : card.received.getTime()));
        }

        String template = _template;
        for (Map.Entry<String, String> entry : subs.entrySet()) {
            template = template.replaceAll(entry.getKey(), entry.getValue());
        }

        PrintWriter out = new PrintWriter(rsp.getOutputStream());
        out.println(template);
        out.close();
    }

    protected String _template;

    @Inject protected EverythingApp _app;
    @Inject protected GameRepository _gameRepo;
    @Inject protected KontagentLogic _kontLogic;
    @Inject protected PlayerRepository _playerRepo;
    @Inject protected ThingRepository _thingRepo;
}
