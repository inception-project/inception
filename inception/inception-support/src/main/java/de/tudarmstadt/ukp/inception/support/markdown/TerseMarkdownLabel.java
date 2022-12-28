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
package de.tudarmstadt.ukp.inception.support.markdown;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

public class TerseMarkdownLabel
    extends Label
{
    private static final long serialVersionUID = 1691095396846449941L;

    public TerseMarkdownLabel(String aId, IModel<?> aModel)
    {
        super(aId, aModel);
    }

    public TerseMarkdownLabel(String aId, Serializable aLabel)
    {
        super(aId, aLabel);
    }

    public TerseMarkdownLabel(String aId)
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
        String markdownString = getDefaultModelObjectAsString();
        String htmlString = MarkdownUtil.markdownToTerseHtml(markdownString);
        htmlString = StringUtils.removeStart(htmlString, "<p>");
        htmlString = StringUtils.removeEnd(htmlString, "</p>");
        replaceComponentTagBody(markupStream, openTag, htmlString);
    }
}
