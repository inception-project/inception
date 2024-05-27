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

public class RootConceptsPanel
    extends Panel
{
    private static final long serialVersionUID = 1161350402387498209L;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;
    private final ListModel<String> concepts;
    private final IModel<String> newConceptIRIString = Model.of();

    private @SpringBean KnowledgeBaseService kbService;

    public RootConceptsPanel(String id, CompoundPropertyModel<KnowledgeBaseWrapper> aModel)
    {
        super(id);

        kbModel = aModel;
        concepts = new ListModel<>(new ArrayList<>(kbModel.getObject().getKb().getRootConcepts()));
        setOutputMarkupId(true);

        ListView<String> conceptsListView = new ListView<String>("rootConcepts", concepts)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<String> item)
            {
                Form<Void> conceptForm = new Form<Void>("conceptForm");
                conceptForm.add(buildTextField("textField", item.getModel()));
                conceptForm.add(new LambdaAjaxLink("removeConcept",
                        t -> RootConceptsPanel.this.actionRemoveConcept(t, item.getModelObject())));
                item.add(conceptForm);
            }

        };
        conceptsListView.setOutputMarkupId(true);
        add(conceptsListView);

        TextField<String> newRootConcept = new TextField<>("newConceptField", newConceptIRIString);
        newRootConcept.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        add(newRootConcept);

        LambdaAjaxLink specifyConcept = new LambdaAjaxLink("newExplicitConcept",
                RootConceptsPanel.this::actionNewRootConcept);
        add(specifyConcept);
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

    private void actionNewRootConcept(AjaxRequestTarget aTarget)
    {
        String concept = newConceptIRIString.getObject();
        if (!isConceptValid(kbModel.getObject().getKb(), concept, true)) {
            error("Concept [" + newConceptIRIString.getObject()
                    + "]  is not an (absolute) IRI, does not exist, or has already been specified");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        concepts.getObject().add(concept);
        kbModel.getObject().getKb().setRootConcepts(concepts.getObject());
        newConceptIRIString.setObject(null);

        aTarget.add(this);
    }

    private void actionRemoveConcept(AjaxRequestTarget aTarget, String iri)
    {
        concepts.getObject().remove(iri);
        kbModel.getObject().getKb().setRootConcepts(concepts.getObject());
        aTarget.add(this);
    }

    public boolean isConceptValid(KnowledgeBase kb, String conceptIRI, boolean aAll)
        throws QueryEvaluationException
    {
        if (isBlank(conceptIRI)) {
            return false;
        }

        return !concepts.getObject().contains(conceptIRI)
                && kbService.readConcept(kbModel.getObject().getKb(), conceptIRI, true).isPresent();
    }
}
