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
import java.util.Optional;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxInstanceSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxNewConceptEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxNewPropertyEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxPropertySelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxStatementChangedEvent;
import de.tudarmstadt.ukp.inception.ui.kb.feature.KnowledgeBaseItemAutoCompleteField;

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
    private @SpringBean ConceptLinkingService conceptLinkingService;

    private IModel<KnowledgeBase> kbModel;
    private Model<KBObject> selectedConceptHandle = Model.of();
    private Model<KBProperty> selectedPropertyHandle = Model.of();
    private Model<KBHandle> searchHandleModel = Model.of();

    private WebMarkupContainer detailContainer;
    private ConceptTreePanel conceptTreePanel;
    private PropertyListPanel propertyListPanel;
    
    private List<String> labelProperties;
    
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
        DropDownChoice<KnowledgeBase> ddc = new BootstrapSelect<KnowledgeBase>("knowledgebases",
                LoadableDetachableModel
                        .of(() -> kbService.getEnabledKnowledgeBases(aProjectModel.getObject())));
        
        ddc.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
            details = details.replaceWith(new EmptyPanel(DETAILS_MARKUP_ID));
            t.add(KnowledgeBasePanel.this);
            t.addChildren(getPage(), IFeedback.class);
        }));
        ddc.setModel(aKbModel);
        ddc.setChoiceRenderer(new ChoiceRenderer<>("name"));
        add(ddc);

        add(createSearchField("searchBar", searchHandleModel, aProjectModel));

        add(conceptTreePanel = new ConceptTreePanel("concepts", kbModel, selectedConceptHandle));
        add(propertyListPanel = new PropertyListPanel("properties", kbModel,
                selectedPropertyHandle));
        
        detailContainer = new WebMarkupContainer(DETAIL_CONTAINER_MARKUP_ID);
        detailContainer.setOutputMarkupId(true);
        add(detailContainer);
        
        details = new EmptyPanel(DETAILS_MARKUP_ID);
        detailContainer.add(details);
    }

    private KnowledgeBaseItemAutoCompleteField createSearchField(String aId,
        IModel<KBHandle> aHandleModel, IModel<Project> aProjectModel)
    {
        KnowledgeBaseItemAutoCompleteField field = new KnowledgeBaseItemAutoCompleteField(
                aId, aHandleModel, _query -> 
                        listSearchResults(aProjectModel.getObject(), _query)) {
            private static final long serialVersionUID = 3188821013226116770L;

            @Override
            protected void onSelected(AjaxRequestTarget aTarget)
            {
                KBHandle selectedResource = this.getModelObject();
                Optional<KBObject> optKbObject = kbService
                    .readItem(kbModel.getObject(), selectedResource.getIdentifier());

                if (optKbObject.isPresent()) {
                    KBObject kbObject = optKbObject.get();
                    sendSelectionChangedEvents(aTarget, kbObject);
                }
            }
        };
        
        field.add(AttributeModifier.append("placeholder",
                new ResourceModel("page.search.placeholder")));
        
        return field;
    }
    /**
     * Search for Entities in the current knowledge base based on a typed string. Use full text
     * search if it is available. Returns a sorted/ranked list of KBHandles
     */
    private List<KBHandle> listSearchResults(Project aProject, String aTypedString)
    {
        List<KBHandle> results;
        KnowledgeBase kb = kbModel.getObject();
        results = conceptLinkingService.searchItems(kb, aTypedString);
        return results;
    }

    /**
     * Send selection-changed events according to type of the selected {@link KBObject}
     */
    private void sendSelectionChangedEvents(AjaxRequestTarget aTarget, KBObject aKbObject) {
        if (aKbObject instanceof KBConcept) {
            send(getPage(), Broadcast.BREADTH,
                new AjaxConceptSelectionEvent(aTarget, KBHandle.of(aKbObject), true));
        }
        else if (aKbObject instanceof KBInstance) {
            List<KBHandle> conceptsForInstance = kbService
                .getConceptForInstance(kbModel.getObject(),
                    aKbObject.getIdentifier(), true);

            if (!conceptsForInstance.isEmpty()) {
                send(getPage(), Broadcast.BREADTH,
                    new AjaxConceptSelectionEvent(aTarget, conceptsForInstance.get(0), true));
            }
            else {
                error("Unable to find the concept to which the the instance ["
                        + aKbObject.getUiLabel() + "] belongs.");
                aTarget.addChildren(getPage(), IFeedback.class);
            }

            send(getPage(), Broadcast.BREADTH,
                new AjaxInstanceSelectionEvent(aTarget, KBHandle.of(aKbObject)));
        }
        else if (aKbObject instanceof KBProperty) {
            send(getPage(), Broadcast.BREADTH,
                new AjaxPropertySelectionEvent(aTarget, (KBProperty) aKbObject, true));
        }
        else {
            throw new IllegalArgumentException(String.format(
                "KBObject must be an instance of one of the following types: [KBConcept, KBInstance, KBProperty], not [%s]",
                aKbObject.getClass().getSimpleName()));
        }
    }
    
    /**
     * Acts upon statement changes. If the changed statement does <strong>not</strong> involve an
     * RDFS or OWL property, the no action is taken. If the changed statement renames the selected
     * concept or property, the name in the respective {@link KBHandle} is updated. For any other
     * statement changes affecting the schema, the page is reloaded via AJAX. Reloading the whole
     * page may not seem like the smartest solution. However, given the severe consequences a single
     * statement change can have (transforming a property into a concept?), it is the simplest
     * working solution.
     */
    @OnEvent
    public void actionStatementChanged(AjaxStatementChangedEvent event)
    {
        // if this event is not about renaming (changing the RDFS label) of a KBObject, return
        KBStatement statement = event.getStatement();

        if (isLabelStatement(statement)) {
            // determine whether the concept name or property name was changed (or neither), then
            // update the name in the respective KBHandle

            List<Model<? extends KBObject>> models = Arrays.asList(selectedConceptHandle,
                    selectedPropertyHandle);
            models.stream().filter(model -> model.getObject() != null && model.getObject()
                    .getIdentifier().equals(statement.getInstance().getIdentifier()))
                    .forEach(model -> {
                        Optional<KBObject> kbObject = kbService
                            .readItem(kbModel.getObject(),
                                model.getObject().getIdentifier());
                        if (kbObject.isPresent()) {
                            model.getObject().setName(kbObject.get().getName());
                        }
                        event.getTarget().add(this);
                    });
        }
        else {
            event.getTarget().add(getPage());
        }
    }

    /**
     * Checks if the given statement is (potentially) assigning the label to the item in subject
     * position. This is the case if the property is a label property. Since we do at this point
     * not know if the statement is about a class, instance or property, we need to check all
     * label properties.
     */
    private boolean isLabelStatement(KBStatement aStatement)
    {
        if (labelProperties == null) {
            labelProperties = kbService.listLabelProperties(kbModel.getObject());
        }
        
        return labelProperties.contains(aStatement.getProperty().getIdentifier());
        
        
//        SimpleValueFactory vf = SimpleValueFactory.getInstance();
//        
//        String propertyIri = aStatement.getProperty().getIdentifier();
//        IRI subjectIri = vf.createIRI(aStatement.getInstance().getIdentifier());
//        IRI labelIri = kbModel.getObject().getLabelIri();
//
//        try (RepositoryConnection conn = kbService.getConnection(kbModel.getObject())) {
//            
//            boolean hasMainLabel = RdfUtils
//                    .readFirst(conn, subjectIri, labelIri, null, kbModel.getObject()).isPresent();
//            
//            return propertyIri.equals(labelIri.stringValue())
//                    || (kbService.isLabelProperty(kbModel.getObject(), propertyIri)
//                            && !hasMainLabel);
//        }
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
                Optional<KBConcept> concept = kbService.readConcept(kbModel.getObject(),
                        selectedConceptHandle.getObject().getIdentifier(), true);
                KBConcept selectedConcept;
                if (concept.isPresent()) {
                    selectedConcept = kbService.readConcept(kbModel.getObject(),
                            selectedConceptHandle.getObject().getIdentifier(), true).get();
                }
                else {
                    selectedConcept = new KBConcept();
                    selectedConcept
                            .setIdentifier(selectedConceptHandle.getObject().getIdentifier());
                }
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
        
        if (event.isRedrawConceptandPropertyListPanels()) {
            event.getTarget().add(conceptTreePanel, propertyListPanel);
        }
        event.getTarget().add(detailContainer);
        event.getTarget().addChildren(getPage(), IFeedback.class);
    }

    @OnEvent
    public void actionNewConcept(AjaxNewConceptEvent event)
    {
        // cancel selections for concepts and properties
        selectedConceptHandle.setObject(null);
        selectedPropertyHandle.setObject(null);

        // show panel for new, empty property
        KBConcept newConcept = new KBConcept();
        newConcept.setLanguage(kbModel.getObject().getDefaultLanguage());
        Component replacement = new ConceptInstancePanel(DETAILS_MARKUP_ID, kbModel,
                selectedConceptHandle, Model.of(newConcept));
        details = details.replaceWith(replacement);

        event.getTarget().add(KnowledgeBasePanel.this);
    }

    @OnEvent
    public void actionPropertySelectionChanged(AjaxPropertySelectionEvent event)
    {
        // cancel selection of concept
        selectedConceptHandle.setObject(null);
        selectedPropertyHandle.setObject(event.getNewSelection());
        
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
        
        if (event.isRedrawConceptandPropertyListPanels()) {
            event.getTarget().add(propertyListPanel, conceptTreePanel);
        }
        event.getTarget().add(detailContainer);
        event.getTarget().addChildren(getPage(), IFeedback.class);
    }

    @OnEvent
    public void actionNewProperty(AjaxNewPropertyEvent event)
    {
        // cancel selections for concepts and properties
        selectedConceptHandle.setObject(null);
        selectedPropertyHandle.setObject(null);

        // show panel for new, empty property
        KBProperty newProperty = new KBProperty();
        newProperty.setLanguage(kbModel.getObject().getDefaultLanguage());
        Component replacement = new PropertyPanel(DETAILS_MARKUP_ID, kbModel,
                selectedPropertyHandle, Model.of(newProperty));
        details = details.replaceWith(replacement);
        event.getTarget().add(KnowledgeBasePanel.this);
    }
}
