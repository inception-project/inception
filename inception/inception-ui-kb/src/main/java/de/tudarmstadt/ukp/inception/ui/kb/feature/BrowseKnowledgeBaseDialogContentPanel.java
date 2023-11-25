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
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import static de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType.ANY_OBJECT;
import static de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType.CONCEPT;
import static de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType.INSTANCE;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.util.Optional;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptNavigateEvent;

public class BrowseKnowledgeBaseDialogContentPanel
    extends Panel
{
    private static final long serialVersionUID = -8498912737302143777L;

    private @SpringBean FeatureSupportRegistry fsRegistry;
    private @SpringBean KnowledgeBaseService kbService;

    private final InstanceListBrowser instanceListBrowser;
    private final ConceptTreeBrowser conceptTreeBrowser;

    private final IModel<KnowledgeBase> knowledgeBase;
    private final IModel<KBObject> concept;
    private final IModel<KBObject> instance;

    public BrowseKnowledgeBaseDialogContentPanel(String aId, IModel<FeatureState> aModel)
    {
        super(aId, aModel);

        var project = aModel.getObject().feature.getProject();

        ConceptFeatureTraits conceptFeatureTraits = fsRegistry
                .readTraits(aModel.getObject().feature, ConceptFeatureTraits::new);

        knowledgeBase = Model.of(findKnowledgeBase(project, conceptFeatureTraits));

        KBObject kbObject = (KBObject) aModel.getObject().value;
        if (knowledgeBase.isPresent().getObject() && kbObject != null
                && !(kbObject instanceof KBInstance || kbObject instanceof KBConcept)) {
            kbObject = kbService.readItem(knowledgeBase.getObject(), kbObject.getIdentifier())
                    .orElse(null);
        }

        instance = Model.of(kbObject instanceof KBInstance ? kbObject : null);
        if (instance.isPresent().getObject()) {
            concept = Model.of(kbService
                    .getConceptForInstance(instance.getObject().getKB(),
                            instance.getObject().getIdentifier(), false)
                    .stream().findFirst().orElse(null));
        }
        else {
            concept = Model.of(kbObject instanceof KBConcept ? kbObject : null);
        }

        instanceListBrowser = new InstanceListBrowser("instances", concept, instance);
        instanceListBrowser.add(visibleWhen(() -> Set.of(ANY_OBJECT, INSTANCE)
                .contains(conceptFeatureTraits.getAllowedValueType())));
        instanceListBrowser.setOutputMarkupPlaceholderTag(true);

        conceptTreeBrowser = new ConceptTreeBrowser("concepts", knowledgeBase, concept);
        conceptTreeBrowser.add(visibleWhen(() -> Set.of(ANY_OBJECT, CONCEPT, INSTANCE)
                .contains(conceptFeatureTraits.getAllowedValueType())));
        conceptTreeBrowser.setConceptNavigationEnabled(
                Set.of(ANY_OBJECT, INSTANCE).contains(conceptFeatureTraits.getAllowedValueType()));
        conceptTreeBrowser.setConceptSelectionEnabled(
                Set.of(ANY_OBJECT, CONCEPT).contains(conceptFeatureTraits.getAllowedValueType()));
        conceptTreeBrowser.setOutputMarkupPlaceholderTag(true);

        queue(instanceListBrowser, conceptTreeBrowser);

        queue(new LambdaAjaxLink("closeDialog", this::actionCancel));
    }

    @OnEvent
    public void onConceptNavigationEvent(AjaxConceptNavigateEvent aEvent)
    {
        instanceListBrowser.refresh();
        aEvent.getTarget().add(instanceListBrowser);
    }

    private KnowledgeBase findKnowledgeBase(Project project,
            ConceptFeatureTraits conceptFeatureTraits)
    {
        if (conceptFeatureTraits.getRepositoryId() != null) {
            Optional<KnowledgeBase> kb = kbService.getKnowledgeBaseById(project,
                    conceptFeatureTraits.getRepositoryId());
            if (kb.isPresent() && kb.get().isEnabled() && kb.get().isSupportConceptLinking()) {
                return kb.get();
            }

            return null;
        }

        return kbService.getEnabledKnowledgeBases(project).stream().findFirst().orElse(null);
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }
}
