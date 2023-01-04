/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils;

import java.io.Serializable;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

public class HighlightLabel
    extends Label
{
    private static final long serialVersionUID = -8785304729150866166L;

    private static final String MARK = "mark";
    private static final String EM = "em";

    public HighlightLabel(String aId, IModel<?> aModel)
    {
        super(aId, aModel);
    }

    public HighlightLabel(String aId, Serializable aLabel)
    {
        super(aId, aLabel);
    }

    public HighlightLabel(String aId)
    {
        super(aId);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
        setEscapeModelStrings(false); // SAFE - SPECIAL HTML-SANITIZING COMPONENT
    }

    @Override
    public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag)
    {
        String highlightString = getDefaultModelObjectAsString();
        String htmlString = cleanHighlight(highlightString);
        replaceComponentTagBody(markupStream, openTag, htmlString);
    }

    public static String cleanHighlight(String aHighlight)
    {
        var safeList = new Safelist();
        safeList.addTags(EM);
        Document dirty = Jsoup.parseBodyFragment(aHighlight, "");
        Cleaner cleaner = new Cleaner(safeList);
        Document clean = cleaner.clean(dirty);
        clean.select(EM).tagName(MARK);

        return clean.body().html();
    }
}
