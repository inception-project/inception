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

import static de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType.CONCEPT;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaChoiceRenderer;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.MultiValueConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;

/**
 * Component for editing the traits of knowledge-base-related features in the feature detail editor
 * of the project settings.
 */
public class MultiValueConceptFeatureTraitsEditor
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
    private @SpringBean ConceptLinkingService conceptLinkingService;

    private String featureSupportId;
    private IModel<AnnotationFeature> feature;
    private CompoundPropertyModel<Traits> traits;

    public MultiValueConceptFeatureTraitsEditor(String aId, MultiValueConceptFeatureSupport aFS,
            IModel<AnnotationFeature> aFeatureModel)
    {
        super(aId, aFeatureModel);

        // We cannot retain a reference to the actual ConceptFeatureSupport instance because that
        // is not serializable, but we can retain its ID and look it up again from the registry
        // when required.
        featureSupportId = aFS.getId();
        feature = aFeatureModel;
        traits = CompoundPropertyModel.of(readTraits());

        Form<Traits> form = new Form<Traits>(MID_FORM, traits)
        {
            private static final long serialVersionUID = -3109239605783291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                writeTraits();
            }
        };

        form.add(new KnowledgeBaseItemAutoCompleteField(MID_SCOPE,
                _query -> listSearchResults(_query, CONCEPT)) //
                        .setOutputMarkupPlaceholderTag(true));

        form.add(new DropDownChoice<>(MID_KNOWLEDGE_BASE,
                LoadableDetachableModel.of(this::listKnowledgeBases),
                new LambdaChoiceRenderer<>(KnowledgeBase::getName)) //
                        .setNullValid(true)
                        .add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::refresh)));
        form.add(new DropDownChoice<>(MID_ALLOWED_VALUE_TYPE,
                LoadableDetachableModel.of(this::listAllowedTypes))
                        .add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::refresh)));

        form.add(new DisabledKBWarning("disabledKBWarning", feature,
                traits.bind("knowledgeBase.repositoryId")));
        add(form);
    }

    private void refresh(AjaxRequestTarget aTarget)
    {
        Traits t = traits.getObject();
        t.setScope(loadConcept(t.getKnowledgeBase(),
                t.getScope() != null ? t.getScope().getIdentifier() : null));
        aTarget.add(get(MID_FORM).get(MID_SCOPE));

    }

    private KBHandle loadConcept(KnowledgeBase aKB, String aIdentifier)
    {
        if (aIdentifier == null) {
            return null;
        }

        // Use the concept from a particular knowledge base
        Optional<KBHandle> scope;
        if (aKB != null) {
            scope = kbService.readHandle(aKB, aIdentifier);
        }
        // Use the concept from any knowledge base (leave KB unselected)
        else {
            scope = kbService.readHandle(feature.getObject().getProject(), aIdentifier);
        }

        return scope.orElse(null);
    }

    /**
     * Read traits and then transfer the values from the actual traits model
     * {{@link ConceptFeatureTraits}} to the the UI traits model ({@link Traits}).
     */
    private Traits readTraits()
    {
        Project project = feature.getObject().getProject();

        Traits result = new Traits();

        MultiValueConceptFeatureTraits t = getFeatureSupport().readTraits(feature.getObject());

        if (t.getRepositoryId() != null) {
            kbService.getKnowledgeBaseById(project, t.getRepositoryId())
                    .ifPresent(result::setKnowledgeBase);
        }

        if (t.getAllowedValueType() != null) {
            result.setAllowedValueType(t.getAllowedValueType());
        }
        else {
            // Allow all values as default
            result.setAllowedValueType(ConceptFeatureValueType.ANY_OBJECT);
        }

        result.setScope(loadConcept(result.getKnowledgeBase(), t.getScope()));

        return result;
    }

    /**
     * Transfer the values from the UI traits model ({@link Traits}) to the actual traits model
     * {{@link ConceptFeatureTraits}} and then store them.
     */
    private void writeTraits()
    {
        MultiValueConceptFeatureTraits t = new MultiValueConceptFeatureTraits();
        if (traits.getObject().knowledgeBase != null) {
            t.setRepositoryId(traits.getObject().knowledgeBase.getRepositoryId());

        }

        if (traits.getObject().scope != null) {
            t.setScope(traits.getObject().scope.getIdentifier());
        }

        t.setAllowedValueType(traits.getObject().allowedValueType);

        getFeatureSupport().writeTraits(feature.getObject(), t);
    }

    private List<KnowledgeBase> listKnowledgeBases()
    {
        return kbService.getEnabledKnowledgeBases(feature.getObject().getProject());
    }

    private List<ConceptFeatureValueType> listAllowedTypes()
    {
        return Arrays.asList(ConceptFeatureValueType.values());
    }

    private MultiValueConceptFeatureSupport getFeatureSupport()
    {
        return (MultiValueConceptFeatureSupport) featureSupportRegistry
                .getExtension(featureSupportId).orElseThrow();
    }

    /**
     * Search for Entities in the current knowledge base based on a typed string. Use full text
     * search if it is available. Returns a sorted/ranked list of KBHandles
     */
    private List<KBHandle> listSearchResults(String aTypedString, ConceptFeatureValueType aType)
    {
        if (StringUtils.isBlank(aTypedString)) {
            return Collections.emptyList();
        }

        Traits t = traits.getObject();
        return conceptLinkingService.getLinkingInstancesInKBScope(
                t.knowledgeBase != null ? t.knowledgeBase.getRepositoryId() : null, null, aType,
                aTypedString, null, -1, null, feature.getObject().getProject());
    }

    /**
     * A UI model holding the traits while the user is editing them. They are read/written to the
     * actual {@link MultiValueConceptFeatureTraits} via
     * {@link MultiValueConceptFeatureTraitsEditor#readTraits()} and
     * {@link MultiValueConceptFeatureTraitsEditor#writeTraits()}.
     */
    private static class Traits
        implements Serializable
    {
        private static final long serialVersionUID = 5804584375190949088L;

        private KnowledgeBase knowledgeBase;
        private KBHandle scope;
        private ConceptFeatureValueType allowedValueType;

        public KBHandle getScope()
        {
            return scope;
        }

        public void setScope(KBHandle aScope)
        {
            scope = aScope;
        }

        public KnowledgeBase getKnowledgeBase()
        {
            return knowledgeBase;
        }

        public void setKnowledgeBase(KnowledgeBase aKnowledgeBase)
        {
            knowledgeBase = aKnowledgeBase;
        }

        @SuppressWarnings("unused")
        public ConceptFeatureValueType getAllowedValueType()
        {
            return allowedValueType;
        }

        public void setAllowedValueType(ConceptFeatureValueType aAllows)
        {
            allowedValueType = aAllows;
        }
    }
}
