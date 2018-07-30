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

import java.net.URI;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.IFeedback;
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

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxInstanceSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxNewInstanceEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxStatementChangedEvent;

public class ConceptInstancePanel
    extends Panel
{

    private static final long serialVersionUID = -1413622323011843523L;
    private static final Logger LOG = LoggerFactory.getLogger(ConceptInstancePanel.class);

    private static final String INSTANCE_INFO_MARKUP_ID = "instanceinfo";

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KnowledgeBase> kbModel;
    private IModel<KBHandle> selectedInstanceHandle;
    private IModel<KBHandle> selectedConceptHandle;

    private Component instanceInfoPanel;
    private Component annotatedSearchPanel;
    

    public ConceptInstancePanel(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<KBHandle> selectedConceptHandle, IModel<KBConcept> selectedConceptModel)
    {
        super(aId, selectedConceptModel);
        setOutputMarkupId(true);
        kbModel = aKbModel;
        selectedInstanceHandle = Model.of();
        this.selectedConceptHandle = selectedConceptHandle;
        add(new ConceptInfoPanel("info", kbModel, selectedConceptHandle, selectedConceptModel));
        add(new InstanceListPanel("instances", kbModel, selectedConceptHandle,
                selectedInstanceHandle));
        if (selectedConceptHandle.getObject() != null) {
            annotatedSearchPanel = new AnnotatedListIdentifiers("annotatedResultGroups", kbModel,
                    selectedConceptHandle, selectedInstanceHandle);
            add(annotatedSearchPanel);
        }
        else {
            annotatedSearchPanel = new EmptyPanel("annotatedResultGroups")
                    .setVisibilityAllowed(false);
            add(annotatedSearchPanel);
        }
        
        instanceInfoPanel = new EmptyPanel(INSTANCE_INFO_MARKUP_ID).setVisibilityAllowed(false);
        add(instanceInfoPanel);
    }

    /**
     * Acts upon statement changes. If the changed statement renames the selected instance, the name
     * in the respective {@link KBHandle} is updated. Otherwise, no action is taken.
     *
     * @param event
     */
    @OnEvent
    public void actionStatementChanged(AjaxStatementChangedEvent event)
    {
        KBStatement statement = event.getStatement();
        KBHandle instanceHandle = selectedInstanceHandle.getObject();

        boolean isRelevantToSelectedInstance = instanceHandle != null
                && instanceHandle.getIdentifier().equals(statement.getInstance().getIdentifier());
        if (!isRelevantToSelectedInstance) {
            return;
        }

        boolean isRenameEvent = statement.getProperty().getIdentifier()
                .equals(RDFS.LABEL.stringValue());
        if (!isRenameEvent) {
            return;
        }

        instanceHandle.setName((String) statement.getValue());
        selectedInstanceHandle.setObject(instanceHandle);
        event.getTarget().add(this);
    }

    @OnEvent
    public void actionInstanceSelection(AjaxInstanceSelectionEvent event)
    {
        selectedInstanceHandle.setObject(event.getSelection());

        // if the instance handle is not null, an existing instance was selected, otherwise it's a
        // deselection
        Component replacementPanel;
        Component replacementSearch;
        if (selectedInstanceHandle.getObject() != null) {
            // load the full KBInstance and display its details in an InstanceInfoPanel
            String identifier = selectedInstanceHandle.getObject().getIdentifier();
            try {
                replacementPanel = kbService.readInstance(kbModel.getObject(), identifier)
                        .<Component>map(instance -> {
                            Model<KBInstance> model = Model.of(instance);
                            return new InstanceInfoPanel(INSTANCE_INFO_MARKUP_ID, kbModel,
                                    selectedInstanceHandle, model);
                        }).orElse(emptyPanel());
                
                replacementSearch = new AnnotatedListIdentifiers("annotatedResultGroups", 
                        kbModel, selectedConceptHandle, selectedInstanceHandle);
                
                
            }
            catch (QueryEvaluationException e) {
                replacementPanel = emptyPanel();
                replacementSearch = emptyPanel();
                error("Unable to read instance: " + e.getLocalizedMessage()); 
                LOG.error("Unable to read instance.", e);
                event.getTarget().addChildren(getPage(), IFeedback.class);
            }
        }
        else {
            replacementPanel = emptyPanel();
            replacementSearch = new AnnotatedListIdentifiers("annotatedResultGroups", kbModel,
                selectedConceptHandle, selectedInstanceHandle);
        }
        annotatedSearchPanel = annotatedSearchPanel.replaceWith(replacementSearch);
        instanceInfoPanel = instanceInfoPanel.replaceWith(replacementPanel);
        event.getTarget().add(this);
    }

    @OnEvent
    public void actionNewInstance(AjaxNewInstanceEvent event)
    {
        // cancel selection in the instance list
        selectedInstanceHandle.setObject(null);

        // show panel for new instance which is a subtype of the currently selected concept
        KBInstance instance = new KBInstance();
        URI type = selectedConceptHandle.getObject() != null
                ? URI.create(selectedConceptHandle.getObject().getIdentifier())
                : null;
        instance.setType(type);

        // replace instance info view
        Component replacement = new InstanceInfoPanel(INSTANCE_INFO_MARKUP_ID, kbModel,
                selectedInstanceHandle, Model.of(instance));
        instanceInfoPanel = instanceInfoPanel.replaceWith(replacement);

        event.getTarget().add(this);
    }

    private Component emptyPanel()
    {
        return new EmptyPanel(INSTANCE_INFO_MARKUP_ID).setVisibilityAllowed(false);
    }
}
