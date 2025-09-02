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

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.model.IModel;
import org.danekja.java.util.function.serializable.SerializableFunction;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.jquery.core.renderer.ITextRenderer;
import org.wicketstuff.jquery.core.renderer.TextRenderer;
import org.wicketstuff.jquery.core.template.IJQueryTemplate;
import org.wicketstuff.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.support.kendo.KendoStyleUtils;

/**
 * Auto-complete field for accessing a knowledge base.
 */
public class KnowledgeBaseItemAutoCompleteField
    extends AutoCompleteTextField<KBHandle>
{
    private static final long serialVersionUID = -1276017349261462683L;

    private SerializableFunction<String, List<KBHandle>> choiceProvider;

    public KnowledgeBaseItemAutoCompleteField(String aId)
    {
        super(aId, new TextRenderer<KBHandle>("uiLabel"));
    }

    public KnowledgeBaseItemAutoCompleteField(String aId,
            SerializableFunction<String, List<KBHandle>> aChoiceProvider)
    {
        super(aId, new TextRenderer<KBHandle>("uiLabel"));

        Validate.notNull(aChoiceProvider);
        choiceProvider = aChoiceProvider;
    }

    public void setChoiceProvider(SerializableFunction<String, List<KBHandle>> aChoiceProvider)
    {
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
    public void onConfigure(JQueryBehavior aBehavior)
    {
        super.onConfigure(aBehavior);

        aBehavior.setOption("ignoreCase", false);
        aBehavior.setOption("delay", 500);
        aBehavior.setOption("animation", false);
        aBehavior.setOption("footerTemplate",
                Options.asString("#: instance.dataSource.total() # items found"));

        KendoStyleUtils.autoDropdownHeight(aBehavior);
        KendoStyleUtils.autoDropdownWidth(aBehavior);
        KendoStyleUtils.resetDropdownSelectionOnOpen(aBehavior);
        KendoStyleUtils.keepDropdownVisibleWhenScrolling(aBehavior);
    }

    @Override
    protected IJQueryTemplate newTemplate()
    {
        return new KBHandleTemplate();
    }
}
