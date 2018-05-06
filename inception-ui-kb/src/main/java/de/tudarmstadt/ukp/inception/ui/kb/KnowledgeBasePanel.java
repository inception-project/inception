/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.ui.kb;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.graph.RdfUtils;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxNewConceptEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxNewPropertyEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxPropertySelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxStatementChangedEvent;

/**
 * Houses the UI for interacting with one knowledge base.<br>
 * 
 * A note on the use of the selection events: If a property is currently selected and the user
 * selects a concept instead, only one event should be broadcast (concept selected), not two
 * (concept selected and property deselected). However, there are cases when property/concept
 * deselection events make sense, which is when a user creates a new property but then decides to
 * cancel the creation process. In this case, neither a property nor a concept should be selected,
 * hence a deselection makes sense to reset the UI.
 */
public class KnowledgeBasePanel
    extends Panel
{

    private static final long serialVersionUID = -3717326058176546655L;
    
    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeBasePanel.class);

    private static final String DETAIL_CONTAINER_MARKUP_ID = "detailContainer";
    private static final String DETAILS_MARKUP_ID = "details";

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KnowledgeBase> kbModel;
    private Model<KBHandle> selectedConceptHandle = Model.of();
    private Model<KBHandle> selectedPropertyHandle = Model.of();

    private WebMarkupContainer detailContainer;
    private ConceptTreePanel conceptTreePanel;
    private PropertyListPanel propertyListPanel;
    
    /**
     * right-side component which either displays concept details or property details
     */
    private Component details;

    public KnowledgeBasePanel(String id, IModel<Project> aProjectModel,
            IModel<KnowledgeBase> aKbModel)
    {
        super(id, aKbModel);

        setOutputMarkupId(true);

        kbModel = aKbModel;
        
        // add the selector for the knowledge bases
        DropDownChoice<KnowledgeBase> ddc = new DropDownChoice<KnowledgeBase>("knowledgebases",
                LambdaModel.of(() -> kbService.getEnabledKnowledgeBases(aProjectModel.getObject())))
        {

            private static final long serialVersionUID = -2635546743813402116L;

            @Override
            public boolean isVisible() {
                // only visible if there is a choice between two or more KBs
                return getChoices().size() >= 2;
            }
        };
        ddc.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
            details = details.replaceWith(new EmptyPanel(DETAILS_MARKUP_ID));
            t.add(KnowledgeBasePanel.this);
            t.addChildren(getPage(), IFeedback.class);
        }));
        ddc.setModel(aKbModel);
        ddc.setChoiceRenderer(new ChoiceRenderer<>("name"));
        add(ddc);

        add(conceptTreePanel = new ConceptTreePanel("concepts", kbModel, selectedConceptHandle));
        add(propertyListPanel = new PropertyListPanel("properties", kbModel,
                selectedPropertyHandle));
        
        detailContainer = new WebMarkupContainer(DETAIL_CONTAINER_MARKUP_ID);
        detailContainer.setOutputMarkupId(true);
        add(detailContainer);
        
        details = new EmptyPanel(DETAILS_MARKUP_ID);
        detailContainer.add(details);
    }
    
    /**
     * Acts upon statement changes. If the changed statement does <strong>not</strong> involve an
     * RDFS or OWL property, the no action is taken. If the changed statement renames the selected
     * concept or property, the name in the respective {@link KBHandle} is updated. For any other
     * statement changes affecting the schema, the page is reloaded via AJAX. Reloading the whole
     * page may not seem like the smartest solution. However, given the severe consequences a single
     * statement change can have (transforming a property into a concept?), it is the simplest
     * working solution.
     *
     * @param event
     */
    @OnEvent
    public void actionStatementChanged(AjaxStatementChangedEvent event)
    {
        boolean isSchemaChangeEvent = RdfUtils
                .isFromImplicitNamespace(event.getStatement().getProperty());
        if (!isSchemaChangeEvent) {
            return;
        }

        // if this event is not about renaming (changing the RDFS label) of a KBObject, return
        KBStatement statement = event.getStatement();
        boolean isRenameEvent = statement.getProperty().getIdentifier()
                .equals(RDFS.LABEL.stringValue());
        if (isRenameEvent) {
            // determine whether the concept name or property name was changed (or neither), then
            // update the name in the respective KBHandle
            List<Model<KBHandle>> models = Arrays.asList(selectedConceptHandle,
                    selectedPropertyHandle);
            models.stream().filter(model -> model.getObject() != null && model.getObject()
                    .getIdentifier().equals(statement.getInstance().getIdentifier()))
                    .forEach(model -> {
                        model.getObject().setName((String) statement.getValue());
                        event.getTarget().add(this);
                    });
        }
        else {
            event.getTarget().add(getPage());
        }
    }

    @OnEvent
    public void actionConceptSelectionChanged(AjaxConceptSelectionEvent event)
    {        
        // cancel selection of property
        selectedPropertyHandle.setObject(null);
        selectedConceptHandle.setObject(event.getSelection());

        // replace detail view: empty panel if a deselection took place (see lengthy explanation
        // above)
        Component replacementPanel;
        if (selectedConceptHandle.getObject() == null) {
            replacementPanel = new EmptyPanel(DETAILS_MARKUP_ID);
        }
        else {
            // TODO: Fix this Optional get() to actual checking
            try {
                KBConcept selectedConcept = kbService.readConcept(kbModel.getObject(),
                        selectedConceptHandle.getObject().getIdentifier()).get();
                replacementPanel = new ConceptInstancePanel(DETAILS_MARKUP_ID, kbModel,
                        selectedConceptHandle, Model.of(selectedConcept));
            }
            catch (QueryEvaluationException e) {
                error("Unable to read concept: " + e.getLocalizedMessage());
                LOG.error("Unable to read concept.", e);
                replacementPanel = new EmptyPanel(DETAILS_MARKUP_ID);

            }
        }
        details = details.replaceWith(replacementPanel);

        event.getTarget().add(conceptTreePanel, propertyListPanel, detailContainer);
        event.getTarget().addChildren(getPage(), IFeedback.class);
    }

    @OnEvent
    public void actionNewConcept(AjaxNewConceptEvent event)
    {
        // cancel selections for concepts and properties
        selectedConceptHandle.setObject(null);
        selectedPropertyHandle.setObject(null);

        // show panel for new, empty property
        Component replacement = new ConceptInstancePanel(DETAILS_MARKUP_ID, kbModel,
                selectedConceptHandle, Model.of(new KBConcept()));
        details = details.replaceWith(replacement);

        event.getTarget().add(KnowledgeBasePanel.this);
    }

    @OnEvent
    public void actionPropertySelectionChanged(AjaxPropertySelectionEvent event)
    {
        // cancel selection of concept
        selectedConceptHandle.setObject(null);
        selectedPropertyHandle.setObject(event.getSelection());

        // replace detail view: empty panel if a deselection took place (see lengthy explanation
        // above)
        Component replacementPanel;
        if (selectedPropertyHandle.getObject() == null) {
            replacementPanel = new EmptyPanel(DETAILS_MARKUP_ID);
        }
        else {
            String identifier = selectedPropertyHandle.getObject().getIdentifier();
            try {
                replacementPanel = kbService.readProperty(kbModel.getObject(), identifier)
                        .<Component>map(selectedProperty -> {
                            Model<KBProperty> model = Model.of(selectedProperty);
                            return new PropertyPanel(DETAILS_MARKUP_ID, kbModel,
                                    selectedPropertyHandle, model);
                        }).orElse(new EmptyPanel(DETAILS_MARKUP_ID));
            }
            catch (QueryEvaluationException e) {
                error("Unable to read property: " + e.getLocalizedMessage());
                LOG.error("Unable to read property.", e);
                replacementPanel = new EmptyPanel(DETAILS_MARKUP_ID);
            }
        }
        details = details.replaceWith(replacementPanel);
        event.getTarget().add(conceptTreePanel, propertyListPanel, detailContainer);
        event.getTarget().addChildren(getPage(), IFeedback.class);
    }

    @OnEvent
    public void actionNewProperty(AjaxNewPropertyEvent event)
    {
        // cancel selections for concepts and properties
        selectedConceptHandle.setObject(null);
        selectedPropertyHandle.setObject(null);

        // show panel for new, empty property
        Component replacement = new PropertyPanel(DETAILS_MARKUP_ID, kbModel,
                selectedPropertyHandle, Model.of(new KBProperty()));
        details = details.replaceWith(replacement);

        event.getTarget().add(KnowledgeBasePanel.this);
    }
}
