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
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.model.IModel;
import org.danekja.java.util.function.serializable.SerializableFunction;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.core.renderer.ITextRenderer;
import com.googlecode.wicket.jquery.core.renderer.TextRenderer;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

/**
 * Auto-complete field for accessing a knowledge base.
 */
public class KnowledgeBaseItemAutoCompleteField
    extends AutoCompleteTextField<KBHandle>
{
    private static final long serialVersionUID = -1276017349261462683L;

    private SerializableFunction<String, List<KBHandle>> choiceProvider;

    public KnowledgeBaseItemAutoCompleteField(String aId,
            SerializableFunction<String, List<KBHandle>> aChoiceProvider)
    {
        super(aId, new TextRenderer<KBHandle>("uiLabel"));

        Validate.notNull(aChoiceProvider);
        choiceProvider = aChoiceProvider;


    }

    public KnowledgeBaseItemAutoCompleteField(String aId,
            SerializableFunction<String, List<KBHandle>> aChoiceProvider,
            ITextRenderer<KBHandle> aRenderer)
    {
        super(aId, aRenderer);

        Validate.notNull(aChoiceProvider);
        choiceProvider = aChoiceProvider;
    }

    public KnowledgeBaseItemAutoCompleteField(String aId, IModel<KBHandle> aModel,
            SerializableFunction<String, List<KBHandle>> aChoiceProvider)
    {
        super(aId, aModel, new TextRenderer<KBHandle>("uiLabel"));

        Validate.notNull(aChoiceProvider);
        choiceProvider = aChoiceProvider;
    }

    @Override
    protected List<KBHandle> getChoices(String aInput)
    {
        return choiceProvider.apply(aInput);
    }

    @Override
    public void onConfigure(JQueryBehavior behavior)
    {
        super.onConfigure(behavior);

        behavior.setOption("ignoreCase", false);
        behavior.setOption("delay", 500);
        behavior.setOption("animation", false);
        behavior.setOption("footerTemplate",
                Options.asString("#: instance.dataSource.total() # items found"));

        // Use one-third of the browser width but not less than 300 pixels. This is better than
        // using the Kendo auto-sizing feature because that sometimes doesn't get the width right.
        behavior.setOption("height", "Math.max($(window).height()*0.5,200)");
        behavior.setOption("open", String.join(" ", "function(e) {",
                "  e.sender.list.width(Math.max($(window).width()*0.3,300));", "}"));

        // Reset the values in the dropdown listbox to avoid that when opening the dropdown the next
        // time ALL items with the same label as the selected item appear as selected
        behavior.setOption("filtering",
                String.join(" ", "function(e) {", "  e.sender.listView.value([]);", "}"));

        // Prevent scrolling action from closing the dropdown while the focus is on the input field
        // The solution we use here is a NASTY hack, but I didn't find any other way to cancel out
        // only the closing triggered by scrolling the browser window without having other adverse
        // side effects such as mouse clicks or enter no longer selecting and closing the dropdown.
        // See: https://github.com/inception-project/inception/issues/1517
        behavior.setOption("close",
                String.join(" ", "function(e) {",
                        "  if (new Error().stack.toString().includes('_resize')) {",
                        "    e.preventDefault();", "  }", "}"));
    }

    @Override
    protected IJQueryTemplate newTemplate()
    {
        return new IJQueryTemplate()
        {
            private static final long serialVersionUID = 8656996525796349138L;

            @Override
            public String getText()
            {
                StringBuilder sb = new StringBuilder();
                sb.append("<div>");
                sb.append("  <div class=\"item-title\">");
                sb.append("  # if (data.rank) { #");
                sb.append("  <span class=\"item-rank\">");
                sb.append("    [${ data.rank }]");
                sb.append("  </span>");
                sb.append("  # } #");
                sb.append("    ${ data.uiLabel }");
                sb.append("  </div>");
                sb.append("  <div class=\"item-identifier\">");
                sb.append("    ${ data.identifier }");
                sb.append("  </div>");
                sb.append("  <div class=\"item-description\">");
                sb.append("    ${ data.description }");
                sb.append("  </div>");
                if (DEVELOPMENT.equals(getApplication().getConfigurationType())) {
                    sb.append("  <div class=\"item-description\">");
                    sb.append("    ${ data.debugInfo }");
                    sb.append("  </div>");
                }
                sb.append("</div>");
                return sb.toString();
            }

            @Override
            public List<String> getTextProperties()
            {
                List<String> properties = new ArrayList<>();
                properties.add("identifier");
                properties.add("description");
                properties.add("rank");
                if (DEVELOPMENT.equals(getApplication().getConfigurationType())) {
                    properties.add("debugInfo");
                }
                return properties;
            }
        };
    }
}
