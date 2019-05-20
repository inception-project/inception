/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import com.googlecode.wicket.jquery.core.renderer.ITextRenderer;
import com.googlecode.wicket.jquery.core.renderer.TextRenderer;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

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

    public KnowledgeBaseItemAutoCompleteField(String aId,
            IModel<KBHandle> aModel,
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

        behavior.setOption("autoWidth", true);
        behavior.setOption("ignoreCase", false);
        behavior.setOption("delay", 500);
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
                sb.append("<div style=\"max-width: 450px\">");
                sb.append("  <div class=\"item-title\">");
                sb.append("    ${ data.name }");
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
                properties.add("name");
                properties.add("identifier");
                properties.add("description");
                if (DEVELOPMENT.equals(getApplication().getConfigurationType())) {
                    properties.add("debugInfo");
                }
                return properties;
            }
        };
    }
}
