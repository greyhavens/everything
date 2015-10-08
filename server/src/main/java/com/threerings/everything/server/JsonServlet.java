//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.server;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

import com.threerings.app.client.ServiceException;

import static com.threerings.everything.Log.log;

public abstract class JsonServlet extends HttpServlet {

    protected abstract Object handle (String method) throws IOException, ServiceException;

    protected void doPost (HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        try {
            _threadReq.set(req);
            _threadRsp.set(rsp);
            String method = req.getPathInfo();
            Object result = handle(method);
            if (result == null) {
                log.warning("Unknown method " + req.getContextPath() + method);
                rsp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown method " + method);
            } else {
                String json = _gson.toJson(result);
                rsp.setContentType("application/json; charset=UTF-8");
                rsp.getWriter().write("OK ");
                rsp.getWriter().write(json);
                // rsp.getOutputStream.write(result.getBytes("UTF8"))
            }

        } catch (ServiceException se) {
            rsp.setContentType("text/plain; charset=UTF-8");
            rsp.getWriter().write("ERR " + se.getMessage());

        } finally {
            _threadReq.remove();
            _threadRsp.remove();
        }
    }

    /** Decodes the request payload as an arguments JSON object. */
    protected <T> T readArgs (Class<T> argClass) throws IOException {
        return _gson.fromJson(threadLocalRequest().getReader(), argClass);
    }

    protected HttpServletRequest threadLocalRequest () {
        return _threadReq.get();
    }
    protected HttpServletResponse threadLocalResponse () {
        return _threadRsp.get();
    }

    protected final Gson _gson = new GsonBuilder().
        setLongSerializationPolicy(LongSerializationPolicy.STRING).
        create();
    protected final ThreadLocal<HttpServletRequest> _threadReq =
        new ThreadLocal<HttpServletRequest>();
    protected final ThreadLocal<HttpServletResponse> _threadRsp =
        new ThreadLocal<HttpServletResponse>();
}
