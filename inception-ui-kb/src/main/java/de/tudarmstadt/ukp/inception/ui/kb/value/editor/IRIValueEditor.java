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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.renderer.TextRenderer;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class IRIValueEditor
    extends ValueEditor
{
    private static final long serialVersionUID = -1646737090861147804L;
    
    private @SpringBean KnowledgeBaseService kbService;
    
    private AutoCompleteTextField<KBHandle> value;

    public IRIValueEditor(String aId, IModel<KBStatement> aModel, IModel<KBProperty> aProperty,
            IModel<KnowledgeBase> kbModel)
    {   
        super(aId, CompoundPropertyModel.of(aModel));
        
        value = new AutoCompleteTextField<KBHandle>("value", Model.of(new KBHandle("","","")) , new TextRenderer<KBHandle>("uiLabel"))
        {
            private static final long serialVersionUID = -1955006051950156603L;
            
            @Override
            protected List<KBHandle> getChoices(String input)
            {
                List<KBHandle> values = new ArrayList<KBHandle>();
                if (aProperty.getObject().getRange() != null) {
                    values.addAll(kbService.listInstances(kbModel.getObject(),
                            aProperty.getObject().getRange(), true));
                    // List of instances for subclasses
                    List<KBHandle> childConcepts = kbService.listChildConcepts(kbModel.getObject(),
                            aProperty.getObject().getRange(), true, 10000);
                    values.addAll(childConcepts);
                    for (KBHandle childConcept : childConcepts) {
                        values.addAll(kbService.listInstances(kbModel.getObject(),
                                childConcept.getIdentifier(), true));
                    }
                    values.add(kbService.readKBIdentifier(kbModel.getObject().getProject(),
                            aProperty.getObject().getRange()).get().toKBHandle());
                }
                
                if (values.isEmpty()) {
                    values = kbService.listConcepts(kbModel.getObject(), true);
                }
                return values;
            }
            
            @Override
            protected void onSelected(AjaxRequestTarget target)
            {
                SimpleValueFactory vf = SimpleValueFactory.getInstance();
                aModel.getObject().setValue(vf.createIRI(this.getModelObject().getIdentifier()));
                super.onSelected(target);
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
