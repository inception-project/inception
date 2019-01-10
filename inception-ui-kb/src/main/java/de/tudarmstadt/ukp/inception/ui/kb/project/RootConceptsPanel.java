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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import java.util.List;

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
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class RootConceptsPanel
    extends Panel
{
    private static final long serialVersionUID = 1161350402387498209L;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;
    private final List<IRI> concepts;
    private final IModel<String> newConceptIRIString = Model.of();

    private @SpringBean KnowledgeBaseService kbService;


    public RootConceptsPanel(String id,
        CompoundPropertyModel<KnowledgeBaseWrapper> aModel)
    {
        super(id);

        kbModel = aModel;
        concepts = kbModel.getObject().getKb().getRootConcepts();
        setOutputMarkupId(true);

        ListView<IRI> conceptsListView = new ListView<IRI>("rootConcepts",
            kbModel.bind("kb.rootConcepts"))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<IRI> item)
            {
                Form<Void> conceptForm = new Form<Void>("conceptForm");
                conceptForm.add(buildTextField("textField", item.getModel()));
                conceptForm.add(new LambdaAjaxLink("removeConcept", t -> {
                    RootConceptsPanel.this.actionRemoveConcept(t, item.getModelObject());
                }));
                item.add(conceptForm);
            }

        };
        conceptsListView.setOutputMarkupId(true);
        add(conceptsListView);

        TextField<String> newRootConcept = new TextField<>("newConceptField",
            newConceptIRIString);
        newRootConcept.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        add(newRootConcept);

        LambdaAjaxLink specifyConcept = new LambdaAjaxLink("newExplicitConcept",
            RootConceptsPanel.this::actionNewRootConcept);
        add(specifyConcept);
    }

    private TextField<String> buildTextField(String id, IModel<IRI> model) {
        IModel<String> adapter = new LambdaModelAdapter<String>(
            () -> model.getObject().stringValue(),
            str -> model.setObject(SimpleValueFactory.getInstance().createIRI(str)));

        TextField<String> iriTextfield = new TextField<>(id, adapter);
        iriTextfield.setOutputMarkupId(true);
        iriTextfield.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
            // Do nothing just update the model values
        }));
        iriTextfield.setEnabled(false);
        return iriTextfield;
    }

    private void actionNewRootConcept(AjaxRequestTarget aTarget) {
        ValueFactory vf = SimpleValueFactory.getInstance();
        IRI concept = vf.createIRI(newConceptIRIString.getObject());
        if (isConceptValid(kbModel.getObject().getKb(), concept, true)) {
            concepts.add(concept);
            newConceptIRIString.setObject(null);
        }
        else {
            error("Concept [" + newConceptIRIString.getObject()
                    + "] does not exist or has already been specified");
            aTarget.addChildren(getPage(), IFeedback.class);
        }
        aTarget.add(this);
    }

    private void actionRemoveConcept(AjaxRequestTarget aTarget, IRI iri) {
        concepts.remove(iri);
        aTarget.add(this);
    }

    public boolean isConceptValid(KnowledgeBase kb, IRI conceptIRI, boolean aAll)
        throws QueryEvaluationException
    {
        return kbService
            .readConcept(kbModel.getObject().getKb(), conceptIRI.stringValue(), true)
            .isPresent()
            && !concepts.contains(conceptIRI);
    }
}
