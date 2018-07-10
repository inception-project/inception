/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.kb.value.editor;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class IRIValueEditor
    extends ValueEditor
{
    private static final long serialVersionUID = -1646737090861147804L;
    
    private @SpringBean KnowledgeBaseService kbService;
    
    AutoCompleteTextField<IRI> value;
    
    public IRIValueEditor(String aId, IModel<KBStatement> aModel, IModel<KnowledgeBase> kbModel)
    {   
        super(aId, CompoundPropertyModel.of(aModel));
        
        value = new AutoCompleteTextField<IRI>("value")
        {
            private static final long serialVersionUID = -1955006051950156603L;
            
            @Override
            protected List<IRI> getChoices(String input)
            {
                SimpleValueFactory vf = SimpleValueFactory.getInstance();
                List<KBHandle> concepts = kbService.listConcepts(kbModel.getObject(), true);
                List<IRI> choices = concepts.stream().map(c -> vf.createIRI(c.getIdentifier()))
                        .collect(Collectors.toList());
                return choices;
            }

            @Override
            public void onConfigure(JQueryBehavior behavior)
            {
                super.onConfigure(behavior);
                behavior.setOption("autoWidth", true);
            }

            @Override
            protected IJQueryTemplate newTemplate()
            {
                return KendoChoiceDescriptionScriptReference.template();
            }
        };
        value.setOutputMarkupId(true);
        value.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> t.add(getParent())));
        add(value);
    }

    @Override
    public Component getFocusComponent()
    {
        return value;
    }

}
