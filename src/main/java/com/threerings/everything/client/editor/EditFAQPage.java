//
// $Id$

package com.threerings.everything.client.editor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.Widgets;

import com.threerings.everything.client.util.Args;
import com.threerings.everything.client.util.Context;
import com.threerings.everything.client.util.Page;

/**
 * A simple page that just displays the editor FAQ.
 */
public class EditFAQPage extends FlowPanel
{
    public EditFAQPage (Context ctx)
    {
        setStyleName("editFAQ");

        add(Widgets.newLabel(_msgs.faqHeader(), "Header", "machine"));

        add(Widgets.newLabel(_msgs.faqBrowserTitle(), "machine"));
        add(Widgets.newHTML(_msgs.faqBrowser()));

        add(Widgets.newLabel(_msgs.faqFactsTitle(), "machine"));
        add(Widgets.newHTML(_msgs.faqFacts()));

        add(Widgets.newLabel(_msgs.faqGrammarTitle(), "machine"));
        add(Widgets.newHTML(_msgs.faqGrammar()));

        add(Widgets.newLabel(_msgs.faqSizeTitle(), "machine"));
        add(Widgets.newHTML(_msgs.faqSize()));

        add(Widgets.newLabel(_msgs.faqSeriesTitle(), "machine"));
        add(Widgets.newHTML(_msgs.faqSeries()));

        add(Widgets.newLabel(_msgs.faqRarityTitle(), "machine"));
        add(Widgets.newHTML(_msgs.faqRarity()));

        add(Widgets.newLabel(_msgs.faqTaxonomyTitle(), "machine"));
        add(Widgets.newHTML(_msgs.faqTaxonomy()));

        add(Widgets.newLabel(_msgs.faqGlyphsTitle(), "machine"));
        add(Widgets.newHTML(_msgs.faqGlyphs()));

        add(Args.createLink("Back to Editor Page", Page.EDIT_CATS));
    }

    protected EditorMessages _msgs = GWT.create(EditorMessages.class);
}
