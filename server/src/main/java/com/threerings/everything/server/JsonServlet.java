//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.server;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.threerings.app.client.ServiceException;

import static com.threerings.everything.Log.log;

public abstract class JsonServlet extends HttpServlet {

    protected abstract Object handle (String method, HttpServletRequest req, HttpServletResponse rsp)
        throws IOException, ServiceException;

    protected void doPost (HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        try {
            String method = req.getPathInfo();
            Object result = handle(method, req, rsp);
            if (result == null) {
                log.warning("Unknown method " + req.getContextPath() + method);
                rsp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown method " + method);
            } else {
                String json = _gson.toJson(result);
                rsp.setContentType("application/json");
                rsp.getWriter().write(json);
            }

        } catch (ServiceException se) {
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se.getMessage());
        }
    }

    /** Decodes the request payload as an arguments JSON object. */
    protected <T> T readArgs (HttpServletRequest req, Class<T> argClass) throws IOException {
        return _gson.fromJson(req.getReader(), argClass);
    }

    protected final Gson _gson = new GsonBuilder().create();
}
