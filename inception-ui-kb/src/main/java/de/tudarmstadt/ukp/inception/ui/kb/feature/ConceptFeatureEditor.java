/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.kendo.ui.form.dropdown.DropDownList;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaChoiceRenderer;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

/**
 * Component for editing knowledge-base-related features on annotations.
 */
public class ConceptFeatureEditor
    extends FeatureEditor
{
    private static final String MID_FEATURE = "feature";
    private static final String MID_VALUE = "value";

    private static final long serialVersionUID = 7763348613632105600L;

    private Component focusComponent;

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    public ConceptFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel)
    {
        super(aId, aItem, new CompoundPropertyModel<>(aModel));
        add(new Label(MID_FEATURE, getModelObject().feature.getUiName()));
        add(focusComponent = createFieldComboBox());
    }

    private DropDownList<KBHandle> createFieldComboBox()
    {
        DropDownList<KBHandle> field = new DropDownList<>(MID_VALUE,
                LambdaModel.of(this::listInstances),
                new LambdaChoiceRenderer<>(KBHandle::getUiLabel));

        // Ensure that markup IDs of feature editor focus components remain constant across
        // refreshes of the feature editor panel. This is required to restore the focus.
        field.setOutputMarkupId(true);
        field.setMarkupId(ID_PREFIX + getModelObject().feature.getId());
        return field;
    }
    
    private List<KBHandle> listInstances()
    {
        AnnotationFeature feat = getModelObject().feature;
        
        List<KBHandle> handles = new ArrayList<>();
        try {
            Project project = feat.getProject();
            FeatureSupport<ConceptFeatureTraits> fs = featureSupportRegistry
                    .getFeatureSupport(feat);
            ConceptFeatureTraits traits = fs.readTraits(feat);

            if (traits.getRepositoryId() != null) {
                // If a specific KB is selected, get its instances
                Optional<KnowledgeBase> kb = kbService.getKnowledgeBaseById(project,
                        traits.getRepositoryId());
                if (kb.isPresent()) {
                    if (traits.getScope() != null) {
                        handles = kbService.listInstances(kb.get(), traits.getScope(), false);
                    }
                    else {
                        for (KBHandle concept : kbService.listConcepts(kb.get(), false)) {
                            handles.addAll(kbService.listInstances(kb.get(),
                                    concept.getIdentifier(), false));
                        }
                    }
                }
            }
            else {
                // If no specific KB is selected, collect instances from all KBs
                for (KnowledgeBase kb : kbService.getKnowledgeBases(project)) {
                    if (traits.getScope() != null) {
                        handles.addAll(kbService.listInstances(kb, traits.getScope(), false));
                    }
                    else {
                        for (KBHandle concept : kbService.listConcepts(kb, false)) {
                            handles.addAll(
                                    kbService.listInstances(kb, concept.getIdentifier(), false));
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            // LOG.error("Unable to read traits", e);
            error("Unable to read traits: " + ExceptionUtils.getRootCauseMessage(e));
            IPartialPageRequestHandler target = RequestCycle.get()
                    .find(IPartialPageRequestHandler.class);
            if (target != null) {
                target.addChildren(getPage(), IFeedback.class);
            }
        }
        return handles;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
    }

    @Override
    public Component getFocusComponent()
    {
        return focusComponent;
    }
}
