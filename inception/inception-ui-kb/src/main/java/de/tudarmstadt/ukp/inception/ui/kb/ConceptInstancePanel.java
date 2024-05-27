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
package de.tudarmstadt.ukp.inception.ui.kb;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.inception.bootstrap.BootstrapAjaxTabbedPanel;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
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
    private IModel<KBObject> selectedInstanceHandle;
    private IModel<KBObject> selectedConceptHandle;

    private Component instanceInfoPanel;

    private List<String> labelProperties;

    public ConceptInstancePanel(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<KBObject> aSelectedConceptHandle, IModel<KBConcept> aSelectedConceptModel)
    {
        super(aId, aSelectedConceptModel);
        setOutputMarkupId(true);
        kbModel = aKbModel;
        selectedInstanceHandle = Model.of();
        selectedConceptHandle = aSelectedConceptHandle;

        add(new BootstrapAjaxTabbedPanel<ITab>("tabPanel", makeTabs()));

        add(new ConceptInfoPanel("info", kbModel, aSelectedConceptHandle, aSelectedConceptModel));

        instanceInfoPanel = new EmptyPanel(INSTANCE_INFO_MARKUP_ID).setVisibilityAllowed(false);
        add(instanceInfoPanel);
    }

    private List<ITab> makeTabs()
    {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new AbstractTab(Model.of("Instances"))
        {
            private static final long serialVersionUID = 6703144434578403272L;

            @Override
            public Panel getPanel(String panelId)
            {
                return new InstanceListPanel(panelId, kbModel, selectedConceptHandle,
                        selectedInstanceHandle);
            }
        });

        tabs.add(new AbstractTab(Model.of("Mentions"))
        {
            private static final long serialVersionUID = 6703144434578403272L;

            @Override
            public Panel getPanel(String panelId)
            {
                if (selectedConceptHandle.getObject() != null) {
                    return new AnnotatedListIdentifiers(panelId, kbModel, selectedConceptHandle,
                            selectedInstanceHandle, false);
                }
                else {
                    return new EmptyPanel(panelId);
                }
            }
        });
        return tabs;
    }

    /**
     * Acts upon statement changes. If the changed statement renames the selected instance, the name
     * in the respective {@link KBHandle} is updated. Otherwise, no action is taken.
     *
     * @param event
     *            the event
     */
    @OnEvent
    public void actionStatementChanged(AjaxStatementChangedEvent event)
    {
        KBStatement statement = event.getStatement();
        KBObject instanceHandle = selectedInstanceHandle.getObject();

        if (instanceHandle == null) {
            return;
        }

        if (!instanceHandle.getIdentifier().equals(statement.getInstance().getIdentifier())) {
            return;
        }

        if (!isLabelStatement(event.getStatement())) {
            return;
        }

        Optional<KBInstance> kbInstance = kbService.readInstance(kbModel.getObject(),
                statement.getInstance().getIdentifier());
        if (kbInstance.isPresent()) {
            instanceHandle.setName(kbInstance.get().getName());
        }
        selectedInstanceHandle.setObject(instanceHandle);
        event.getTarget().add(this);
    }

    // /**
    // * Checks whether the given event is about renaming a knowledge base instance i.e. checks
    // * whether the label of an instance has been changed in the event.
    // * An event is considered a renaming event if the changed property is:
    // *
    // * a main label (declared with {@link KnowledgeBase#getLabelIri()})
    // * or
    // * a subproperty label and there is no main label present for this instance
    // *
    // * @param aEvent the event that is checked
    // * @return true if the event is a renaming event, false otherwise
    // */
    // private boolean isRenamingEvent(AjaxStatementChangedEvent aEvent)
    // {
    // KBStatement changedStatement = aEvent.getStatement();
    // String propertyIdentifier = changedStatement.getProperty().getIdentifier();
    // SimpleValueFactory vf = SimpleValueFactory.getInstance();
    // boolean hasMainLabel = RdfUtils.readFirst(kbService.getConnection(kbModel.getObject()),
    // vf.createIRI(changedStatement.getInstance().getIdentifier()),
    // kbModel.getObject().getLabelIri(), null, kbModel.getObject()).isPresent();
    // return propertyIdentifier.equals(kbModel.getObject().getLabelIri().stringValue()) || (
    // kbService.isLabelProperty(kbModel.getObject(), propertyIdentifier)
    // && !hasMainLabel);
    // }

    /**
     * Checks if the given statement is (potentially) assigning the label to the item in subject
     * position. This is the case if the property is a label property.
     */
    private boolean isLabelStatement(KBStatement aStatement)
    {
        if (labelProperties == null) {
            labelProperties = kbService.listConceptOrInstanceLabelProperties(kbModel.getObject());
        }

        return labelProperties.contains(aStatement.getProperty().getIdentifier());
    }

    @OnEvent
    public void actionInstanceSelection(AjaxInstanceSelectionEvent event)
    {
        selectedInstanceHandle.setObject(event.getSelection());

        // if the instance handle is not null, an existing instance was selected, otherwise it's a
        // deselection
        Component replacementPanel;

        if (selectedInstanceHandle.getObject() != null) {
            // load the full KBInstance and display its details in an InstanceInfoPanel
            String identifier = selectedInstanceHandle.getObject().getIdentifier();
            try {
                replacementPanel = kbService.readInstance(kbModel.getObject(), identifier)
                        .<Component> map(instance -> {
                            Model<KBInstance> model = Model.of(instance);
                            return new InstancePanel(INSTANCE_INFO_MARKUP_ID, kbModel,
                                    selectedConceptHandle, selectedInstanceHandle, model);
                        }).orElse(emptyPanel());
            }
            catch (QueryEvaluationException e) {
                replacementPanel = emptyPanel();
                // replacementSearch = emptyPanel();
                error("Unable to read instance: " + e.getLocalizedMessage());
                LOG.error("Unable to read instance.", e);
                event.getTarget().addChildren(getPage(), IFeedback.class);
            }
        }
        else {
            replacementPanel = emptyPanel();
        }
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
        instance.setLanguage(kbModel.getObject().getDefaultLanguage());

        // replace instance info view
        Component replacement = new InstancePanel(INSTANCE_INFO_MARKUP_ID, kbModel,
                selectedConceptHandle, selectedInstanceHandle, Model.of(instance));
        instanceInfoPanel = instanceInfoPanel.replaceWith(replacement);

        event.getTarget().add(this);
    }

    private Component emptyPanel()
    {
        return new EmptyPanel(INSTANCE_INFO_MARKUP_ID).setVisibilityAllowed(false);
    }
}
