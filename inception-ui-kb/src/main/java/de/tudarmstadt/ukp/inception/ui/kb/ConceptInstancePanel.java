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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

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
    extends EventListeningPanel
{

    private static final long serialVersionUID = -1413622323011843523L;

    private static final String INSTANCE_INFO_MARKUP_ID = "instanceinfo";

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KnowledgeBase> kbModel;
    private IModel<KBHandle> selectedInstanceHandle;
    private IModel<KBHandle> selectedConceptHandle;

    private Component instanceInfoPanel;

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

        instanceInfoPanel = new EmptyPanel(INSTANCE_INFO_MARKUP_ID).setVisibilityAllowed(false);
        add(instanceInfoPanel);

        eventHandler.addCallback(AjaxInstanceSelectionEvent.class, this::actionInstanceSelection);
        eventHandler.addCallback(AjaxNewInstanceEvent.class, this::actionNewInstance);
        eventHandler.addCallback(AjaxStatementChangedEvent.class, this::actionStatementChanged);
    }

    /**
     * Acts upon statement changes. If the changed statement renames the selected instance, the name
     * in the respective {@link KBHandle} is updated. Otherwise, no action is taken.
     *
     * @param target
     * @param event
     */
    private void actionStatementChanged(AjaxRequestTarget target,
            AjaxStatementChangedEvent event)
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
        target.add(this);
    }

    private void actionInstanceSelection(AjaxRequestTarget target, AjaxInstanceSelectionEvent event)
    {
        selectedInstanceHandle.setObject(event.getSelection());

        // if the instance handle is not null, an existing instance was selected, otherwise it's a
        // deselection
        Component replacementPanel;
        if (selectedInstanceHandle.getObject() != null) {
            // load the full KBInstance and display its details in an InstanceInfoPanel
            String identifier = selectedInstanceHandle.getObject().getIdentifier();
            replacementPanel = kbService.readInstance(kbModel.getObject(), identifier)
                    .<Component>map(instance -> {
                        Model<KBInstance> model = Model.of(instance);
                        return new InstanceInfoPanel(INSTANCE_INFO_MARKUP_ID, kbModel,
                                selectedInstanceHandle, model);
                    }).orElse(emptyPanel());
        }
        else {
            replacementPanel = emptyPanel();
        }
        instanceInfoPanel = instanceInfoPanel.replaceWith(replacementPanel);
        target.add(this);
    }

    private void actionNewInstance(AjaxRequestTarget target, AjaxNewInstanceEvent event)
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

        target.add(this);
    }

    private Component emptyPanel()
    {
        return new EmptyPanel(INSTANCE_INFO_MARKUP_ID).setVisibilityAllowed(false);
    }
}
