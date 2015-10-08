//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.client.admin;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.LimitedTextArea;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Value;

import com.threerings.everything.rpc.AdminService;
import com.threerings.everything.rpc.AdminServiceAsync;
import com.threerings.everything.data.News;

import com.threerings.everything.client.util.ClickCallback;
import com.threerings.everything.client.util.Context;

/**
 * Displays an interface for editing the game news.
 */
public class EditNewsPage extends FlowPanel
{
    public EditNewsPage (Context ctx, Value<News> news)
    {
        setStyleName("page");
        _ctx = ctx;
        _news = news;

        final LimitedTextArea onews = new LimitedTextArea(News.MAX_NEWS_LENGTH, 80, 5);
        final Button upnews = new Button("Update");
        new ClickCallback<Void>(upnews) {
            protected boolean callService () {
                _news.get().text = onews.getText().trim();
                _adminsvc.updateNews(_news.get().reported.getTime(), _news.get().text, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                Popups.infoBelow("News updated.", upnews);
                _news.update(_news.get());
                return true;
            }
        };

        final LimitedTextArea nnews = new LimitedTextArea(News.MAX_NEWS_LENGTH, 80, 5);
        final Button post = new Button("Post");
        new ClickCallback<Long>(post) {
            protected boolean callService () {
                String text = nnews.getText().trim();
                if (text.length() == 0) {
                    return false;
                }
                _adminsvc.addNews(text, this);
                return true;
            }
            protected boolean gotResult (Long reported) {
                News news = new News();
                news.reported = new Date(reported);
                news.reporter = _ctx.getMe();
                news.text = nnews.getText().trim();
                _news.update(news);
                setLatestNews(onews, upnews, news);
                nnews.setText("");
                Popups.infoBelow("News posted.", post);
                return true;
            }
        };

        add(Widgets.newLabel("Add News", "machine"));
        add(nnews);
        add(post);

        add(Widgets.newShim(10, 10));

        add(Widgets.newLabel("Latest News", "machine"));
        add(onews);
        add(upnews);
        setLatestNews(onews, upnews, news.get());
    }

    protected void setLatestNews (LimitedTextArea text, Button action, News news)
    {
        if (news != null) {
            text.setText(news.text);
        }
        text.getTextArea().setEnabled(news != null);
        action.setEnabled(news != null);
    }

    protected Context _ctx;
    protected Value<News> _news;

    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);
}
