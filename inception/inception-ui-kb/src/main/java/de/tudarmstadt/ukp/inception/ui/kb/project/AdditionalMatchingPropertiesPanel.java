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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class AdditionalMatchingPropertiesPanel
    extends Panel
{
    private static final long serialVersionUID = 1161350402387498209L;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;
    private final ListModel<String> properties;
    private final IModel<String> newPropertyIRIString = Model.of();

    private @SpringBean KnowledgeBaseService kbService;

    public AdditionalMatchingPropertiesPanel(String id,
            CompoundPropertyModel<KnowledgeBaseWrapper> aModel)
    {
        super(id);

        kbModel = aModel;
        properties = new ListModel<>(
                new ArrayList<>(kbModel.getObject().getKb().getAdditionalMatchingProperties()));
        setOutputMarkupId(true);

        ListView<String> additionalMatchingPropertiesListView = new ListView<String>(
                "additionalMatchingProperties", properties)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<String> item)
            {
                Form<Void> propertyForm = new Form<Void>("propertyForm");
                propertyForm.add(buildTextField("textField", item.getModel()));
                propertyForm.add(new LambdaAjaxLink("removeProperty",
                        t -> AdditionalMatchingPropertiesPanel.this.actionRemoveProperty(t,
                                item.getModelObject())));
                item.add(propertyForm);
            }

        };
        additionalMatchingPropertiesListView.setOutputMarkupId(true);
        add(additionalMatchingPropertiesListView);

        TextField<String> newPropertyField = new TextField<>("newPropertyField",
                newPropertyIRIString);
        newPropertyField.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        add(newPropertyField);

        LambdaAjaxLink addPropertyButton = new LambdaAjaxLink("addProperty",
                AdditionalMatchingPropertiesPanel.this::actionAddProperty);
        add(addPropertyButton);
    }

    private TextField<String> buildTextField(String id, IModel<String> model)
    {
        TextField<String> iriTextfield = new TextField<>(id, model);
        iriTextfield.setOutputMarkupId(true);
        iriTextfield.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
            // Do nothing just update the model values
        }));
        iriTextfield.setEnabled(false);
        return iriTextfield;
    }

    private void actionAddProperty(AjaxRequestTarget aTarget)
    {
        String concept = newPropertyIRIString.getObject();

        if (!isPropertyValid(kbModel.getObject().getKb(), concept, true)) {
            error("Property [" + newPropertyIRIString.getObject()
                    + "] is not an (absolute) IRI, does not exist, or has already been specified");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        properties.getObject().add(concept);
        kbModel.getObject().getKb().setAdditionalMatchingProperties(properties.getObject());
        newPropertyIRIString.setObject(null);
        aTarget.add(this);
    }

    private void actionRemoveProperty(AjaxRequestTarget aTarget, String iri)
    {
        properties.getObject().remove(iri);
        kbModel.getObject().getKb().setAdditionalMatchingProperties(properties.getObject());
        aTarget.add(this);
    }

    public boolean isPropertyValid(KnowledgeBase aKb, String aPropertyIri, boolean aAll)
        throws QueryEvaluationException
    {
        if (isBlank(aPropertyIri)) {
            return false;
        }

        // Check if it looks like an (absolute) IRI
        if (aPropertyIri.indexOf(':') < 0) {
            return false;
        }

        return !properties.getObject().contains(aPropertyIri);
        // KBs tend to not declare all their properties, so we do not try to check if it actually
        // exist...
        // && kbService.readProperty(kbModel.getObject().getKb(), aPropertyIri).isPresent();
    }
}
