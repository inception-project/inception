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
package de.tudarmstadt.ukp.inception.support.kendo;

import static java.util.Arrays.asList;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.wicketstuff.jquery.core.template.IJQueryTemplate;
import org.wicketstuff.jquery.ui.settings.JQueryUILibrarySettings;

public class KendoChoiceDescriptionScriptReference
    extends JavaScriptResourceReference
{
    private static final long serialVersionUID = 1L;

    private static final KendoChoiceDescriptionScriptReference INSTANCE = new KendoChoiceDescriptionScriptReference();

    /**
     * Gets the instance of the resource reference
     *
     * @return the single instance of the resource reference
     */
    public static KendoChoiceDescriptionScriptReference get()
    {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private KendoChoiceDescriptionScriptReference()
    {
        super(KendoChoiceDescriptionScriptReference.class,
                "KendoChoiceDescriptionScriptReference.js");
    }

    @Override
    public List<HeaderItem> getDependencies()
    {
        List<HeaderItem> dependencies = new ArrayList<>(super.getDependencies());

        // Required to load the tooltop plugin
        dependencies.add(forReference(JQueryUILibrarySettings.get().getJavaScriptReference()));

        return dependencies;
    }

    public static String applyTooltipScript()
    {
        return "(evt) => { applyTooltip(evt.sender.list) }";
    }

    public static IJQueryTemplate template()
    {
        return new IJQueryTemplate()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public String getText()
            {
                StringBuilder sb = new StringBuilder();
                sb.append("<span class='item-title' title='#: data.description #'>");
                sb.append("${ data.name }");
                sb.append("</span>");
                return sb.toString();
            }

            @Override
            public List<String> getTextProperties()
            {
                return asList("name", "description");
            }
        };
    }

    public static IJQueryTemplate templateReorderable()
    {
        return new IJQueryTemplate()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public String getText()
            {
                // Docs on how the templates work in Kendo, in case we need more fancy dropdowns
                // http://docs.telerik.com/kendo-ui/framework/templates/overview
                // @formatter:off
                StringBuilder sb = new StringBuilder();
                sb.append("<span class='d-inline' title='#: data.description #'>");
                sb.append("# if (data.reordered == 'true') { #");
                sb.append("<b>${data.name}</b>");
                sb.append("# } else { #");
                sb.append("${data.name}");
                sb.append("# } #");
                sb.append("</span>");
                sb.append("# if (data.score) { #");
                sb.append("<span class='float-end fw-lighter font-monospace small w-auto d-inline'>");
                sb.append("${ data.score }");
                sb.append("</span>");
                sb.append("# } #");
                // @formatter:on

                return sb.toString();
            }

            @Override
            public List<String> getTextProperties()
            {
                return asList("name", "description", "reordered", "score");
            }
        };
    }
}
