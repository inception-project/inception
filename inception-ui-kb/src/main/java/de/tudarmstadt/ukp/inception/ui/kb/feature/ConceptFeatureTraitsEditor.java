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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaChoiceRenderer;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

/**
 * Component for editing the traits of knowledge-base-related features in the feature detail editor
 * of the project settings.
 */
public class ConceptFeatureTraitsEditor
    extends Panel
{
    private static final String MID_FORM = "form";

    private static final String MID_KNOWLEDGE_BASE = "knowledgeBase";

    private static final String MID_SCOPE = "scope";

    private static final String MID_ALLOWED_VALUE_TYPE = "allowedValueType";

    private static final long serialVersionUID = 2129000875921279514L;
    
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean KnowledgeBaseService kbService;

    private final String featureSupportId;
    private final AnnotationFeature feature;
    private final IModel<Traits> traits;
    
    public ConceptFeatureTraitsEditor(String aId, ConceptFeatureSupport aFS,
            IModel<AnnotationFeature> aFeatureModel)
    {
        super(aId, aFeatureModel);
        
        // We cannot retain a reference to the actual ConceptFeatureSupport instance because that
        // is not serializable, but we can retain its ID and look it up again from the registry
        // when required.
        featureSupportId = aFS.getId();
        feature = aFeatureModel.getObject();
        traits = Model.of(readTraits());

        Form<Traits> form = new Form<Traits>(MID_FORM, CompoundPropertyModel.of(traits))
        {
            private static final long serialVersionUID = -3109239605783291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                writeTraits();
            }
        };
        
        form.add(
                new DropDownChoice<>(MID_SCOPE, 
                        LambdaModel.of(this::listConcepts),
                        new LambdaChoiceRenderer<>(KBHandle::getUiLabel))
                .setNullValid(true)
                .setOutputMarkupPlaceholderTag(true));

        form.add(
                new DropDownChoice<>(MID_KNOWLEDGE_BASE, 
                        LambdaModel.of(this::listKnowledgeBases), 
                        new LambdaChoiceRenderer<>(KnowledgeBase::getName))
                .setNullValid(true)
                .add(new LambdaAjaxFormComponentUpdatingBehavior("change", target ->
                        target.add(form.get(MID_SCOPE)))));
        form.add(
            new DropDownChoice<>(MID_ALLOWED_VALUE_TYPE, LambdaModel.of(this::listAllowedTypes)));

        add(form);
    }
    
    /**
     * Read traits and then transfer the values from the actual traits model
     * {{@link ConceptFeatureTraits}} to the the UI traits model ({@link Traits}).
     */
    private Traits readTraits()
    {
        Traits result = new Traits();

        Project project = feature.getProject();

        ConceptFeatureTraits t = getFeatureSupport().readTraits(feature);

        // Use the concept from a particular knowledge base
        if (t.getRepositoryId() != null) {
            Optional<KnowledgeBase> kb = kbService.getKnowledgeBaseById(project,
                    t.getRepositoryId());
            if (kb.isPresent()) {
                result.setKnowledgeBase(kb.get());
                if (t.getScope() != null) {
                    kbService.readConcept(kb.get(), t.getScope(), true)
                            .ifPresent(concept -> result.setScope(KBHandle.of(concept)));
                }
            }
        }
        // Use the concept from any knowledge base (leave KB unselected)
        else if (t.getScope() != null) {
            kbService.readConcept(project, t.getScope())
                    .ifPresent(concept -> result.setScope(KBHandle.of(concept)));
        }

        if (t.getAllowedValueType() != null) {
            result.setAllowedValueType(t.getAllowedValueType());
        }
        else {
            // Allow all values as default
            result.setAllowedValueType(ConceptFeatureValueType.ANY_OBJECT);
        }

        return result;
    }
    
    /**
     * Transfer the values from the UI traits model ({@link Traits}) to the actual traits model
     * {{@link ConceptFeatureTraits}} and then store them.
     */
    private void writeTraits()
    {
        ConceptFeatureTraits t = new ConceptFeatureTraits();
        if (traits.getObject().knowledgeBase != null) {
            t.setRepositoryId(traits.getObject().knowledgeBase.getRepositoryId());
        
        }
        
        if (traits.getObject().scope != null) {
            t.setScope(traits.getObject().scope.getIdentifier());
        }

        t.setAllowedValueType(traits.getObject().allowedValueType);

        getFeatureSupport().writeTraits(feature, t);
    }
    
    private List<KnowledgeBase> listKnowledgeBases()
    {
        return kbService.getKnowledgeBases(feature.getProject());
    }
    
    private List<KBHandle> listConcepts()
    {
        // If a specific KB is selected, we show the concepts inside that one
        if (traits.getObject().knowledgeBase != null) {
            return kbService.listConcepts(traits.getObject().knowledgeBase, false);
        }
        // Otherwise, we offer concepts from all KBs
        else {
            List<KBHandle> allConcepts = new ArrayList<>();
            for (KnowledgeBase kb : kbService.getKnowledgeBases(feature.getProject())) {
                allConcepts.addAll(kbService.listConcepts(kb, false));
            }

            return allConcepts;
        }
    }

    private List<ConceptFeatureValueType> listAllowedTypes() {
        return Arrays.asList(ConceptFeatureValueType.values());
    }
    
    private ConceptFeatureSupport getFeatureSupport()
    {
        return featureSupportRegistry.getFeatureSupport(featureSupportId);
    }
    
    /**
     * A UI model holding the traits while the user is editing them. They are read/written to the
     * actual {@link ConceptFeatureTraits} via {@link ConceptFeatureTraitsEditor#readTraits()} and
     * {@link ConceptFeatureTraitsEditor#writeTraits()}.
     */
    private static class Traits implements Serializable
    {
        private static final long serialVersionUID = 5804584375190949088L;

        private KnowledgeBase knowledgeBase;
        private KBHandle scope;
        private ConceptFeatureValueType allowedValueType;

        @SuppressWarnings("unused")
        public KBHandle getScope()
        {
            return scope;
        }

        public void setScope(KBHandle aScope)
        {
            scope = aScope;
        }

        @SuppressWarnings("unused")
        public KnowledgeBase getKnowledgeBase()
        {
            return knowledgeBase;
        }

        public void setKnowledgeBase(KnowledgeBase aKnowledgeBase)
        {
            knowledgeBase = aKnowledgeBase;
        }

        public ConceptFeatureValueType getAllowedValueType() {
            return allowedValueType;
        }

        public void setAllowedValueType(ConceptFeatureValueType aAllows) {
            allowedValueType = aAllows;
        }
    }
}
