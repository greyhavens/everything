//
// $Id$

package com.threerings.everything.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.google.code.facebookapi.schema.User;

import com.samskivert.util.StringUtil;

import com.threerings.everything.client.Kontagent;
import com.threerings.everything.server.persist.PlayerRecord;
import com.threerings.samsara.app.data.AppCodes;
import static com.threerings.everything.Log.log;

/**
 * Handles interacting with Kontagent.
 */
@Singleton
public class KontagentLogic
{
    /**
     * Generates a unique id to use for Kontagent invite and notification tracking.
     */
    public String generateUniqueId (int forUserId)
    {
        int now = (int)(System.currentTimeMillis() % Integer.MAX_VALUE);
        int rando = _rando.nextInt(), nodeId = _nodeId << 24;
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.asIntBuffer().put(forUserId | nodeId).put(now ^ rando);
        return StringUtil.hexlate(buf.array());
    }

    /**
     * Reports a tracking action to Kontagent.
     */
    public void reportAction (Kontagent type, Object... argVals)
    {
        final String url = _app.getKontagentURL(type, argVals);
        _app.getExecutor().execute(new Runnable() {
            public void run () {
                try {
                    BufferedReader ins = new BufferedReader(
                        new InputStreamReader(new URL(url).openStream()));
                    String rsp = ins.readLine();
                    if (!"OK".equals(rsp) && !"1".equals(rsp)) {
                        log.warning("Kontagent rejected action", "rsp", rsp);
                    } else {
                        log.info("Kontagent action accepted", "url", url);
                    }
                    ins.close();
                } catch (Exception e) {
                    log.warning("Kontagent action failed", "url", url, "e", e);
                }
            }
        });
    }

    /**
     * Reports data on a new app user to Kontagent. The supplied fbuser object should contain the
     * following profile fields: BIRTHDAY, SEX, CURRENT_LOCATION.
     */
    public void reportNewUser (PlayerRecord user, User fbuser, String tracking)
    {
        // report that this user added our app
        reportAction(Kontagent.APP_ADDED, "s", user.facebookId,
                     (tracking.length() == 8) ? "su" : "u", tracking);

        // report profile related information on this user to Kontagent
        List<Object> args = Lists.newArrayList();
        add(args, "s", user.facebookId);

        String sex = fbuser.getSex();
        if (sex.equalsIgnoreCase("male")) {
            add(args, "g", "m");
        } else if (sex.equalsIgnoreCase("female")) {
            add(args, "g", "f");
        } else {
            add(args, "g", "u");
        }

        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(_bfmt.parse(fbuser.getBirthday()));
            add(args, "b", cal.get(Calendar.YEAR));
        } catch (Exception e) {
            // oh well
        }

        if (fbuser.getCurrentLocation() != null) {
            add(args, "ly", fbuser.getCurrentLocation().getCity());
            add(args, "ls", fbuser.getCurrentLocation().getState());
            add(args, "lc", fbuser.getCurrentLocation().getCountry());
            add(args, "lp", fbuser.getCurrentLocation().getZip());
        }

        // TODO: friend count?

        reportAction(Kontagent.USER_INFO, args.toArray(new Object[args.size()]));
    }

    protected static void add (List<Object> args, String code, Object value)
    {
        if (!StringUtil.isBlank(String.valueOf(value))) {
            args.add(code);
            args.add(value);
        }
    }

    protected Random _rando = new Random();

    @Inject @Named(AppCodes.NODE_ID) protected int _nodeId;
    @Inject protected EverythingApp _app;

    protected static SimpleDateFormat _bfmt = new SimpleDateFormat("MMMM dd, yyyy");
}
