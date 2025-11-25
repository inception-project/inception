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
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.LambdaChoiceRenderer;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBindingsConfigurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.RecommendableFeatureTrait;
import de.tudarmstadt.ukp.inception.schema.api.feature.RetainSuggestionInfoPanel;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

/**
 * Component for editing the traits of knowledge-base-related features in the feature detail editor
 * of the project settings.
 */
public class ConceptFeatureTraitsEditor
    extends GenericPanel<AnnotationFeature>
{
    private static final String MID_KEY_BINDINGS = "keyBindings";
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
    private CompoundPropertyModel<ConceptFeatureTraits> traits;

    public ConceptFeatureTraitsEditor(String aId, ConceptFeatureSupport aFS,
            IModel<AnnotationFeature> aFeatureModel)
    {
        super(aId, aFeatureModel);

        // We cannot retain a reference to the actual ConceptFeatureSupport instance because that
        // is not serializable, but we can retain its ID and look it up again from the registry
        // when required.
        featureSupportId = aFS.getId();
        feature = aFeatureModel;

        traits = CompoundPropertyModel.of(getFeatureSupport().readTraits(feature.getObject()));
        if (traits.getObject().getAllowedValueType() == null) {
            traits.getObject().setAllowedValueType(ConceptFeatureValueType.ANY_OBJECT);
        }

        var form = new Form<ConceptFeatureTraits>(MID_FORM, traits)
        {
            private static final long serialVersionUID = -3109239605783291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                getFeatureSupport().writeTraits(feature.getObject(), traits.getObject());
            }
        };
        add(form);

        var scope = new KnowledgeBaseItemAutoCompleteField(MID_SCOPE);
        scope.setModel(LambdaModel.of(this::getScope, this::setScope));
        scope.setChoiceProvider(_query -> listSearchResults(_query, CONCEPT));
        scope.setOutputMarkupPlaceholderTag(true);
        form.add(scope);

        var knowledgeBase = new DropDownChoice<KnowledgeBase>(MID_KNOWLEDGE_BASE);
        knowledgeBase.setModel(
                LambdaModel.of(this::getSelectedKnowledgeBase, this::setSelectedKnowledgeBase));
        knowledgeBase.setChoices(LoadableDetachableModel.of(this::listKnowledgeBases));
        knowledgeBase.setChoiceRenderer(new LambdaChoiceRenderer<>(KnowledgeBase::getName));
        knowledgeBase.setNullValid(true);
        knowledgeBase.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT, this::refresh));
        form.add(knowledgeBase);

        form.add(new DropDownChoice<>(MID_ALLOWED_VALUE_TYPE,
                LoadableDetachableModel.of(this::listAllowedTypes)).add(
                        new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT, this::refresh)));

        form.add(new DisabledKBWarning("disabledKBWarning", feature,
                traits.map(ConceptFeatureTraits::getRepositoryId)));

        form.add(new RetainSuggestionInfoPanel("retainSuggestionInfo", aFeatureModel,
                traits.map(RecommendableFeatureTrait.class::cast)));

        add(new KeyBindingsConfigurationPanel(MID_KEY_BINDINGS, aFeatureModel,
                traits.bind(MID_KEY_BINDINGS)).setOutputMarkupId(true));

    }

    private KBHandle getScope()
    {
        if (!traits.isPresent().getObject()) {
            return null;
        }

        var kb = getSelectedKnowledgeBase();
        var scope = traits.getObject().getScope();
        return loadConcept(kb, scope);
    }

    private void setScope(KBHandle aScope)
    {
        if (traits.isPresent().getObject()) {
            traits.getObject().setScope(aScope != null ? aScope.getIdentifier() : null);
        }

    }

    private KnowledgeBase getSelectedKnowledgeBase()
    {
        var project = getModelObject().getProject();
        return traits.map(ConceptFeatureTraits::getRepositoryId) //
                .map(id -> kbService.getKnowledgeBaseById(project, id).orElse(null)) //
                .orElse(null) //
                .getObject();

    }

    private void setSelectedKnowledgeBase(KnowledgeBase aKB)
    {
        if (traits.isPresent().getObject()) {
            traits.getObject().setRepositoryId(aKB != null ? aKB.getRepositoryId() : null);
        }
    }

    private void refresh(AjaxRequestTarget aTarget)
    {
        setScope(getScope()); // Make sure the scope belongs to the selected KB
        aTarget.add(get(MID_FORM).get(MID_SCOPE), get(MID_KEY_BINDINGS));
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

    private List<KnowledgeBase> listKnowledgeBases()
    {
        return kbService.getEnabledKnowledgeBases(feature.getObject().getProject());
    }

    private List<ConceptFeatureValueType> listAllowedTypes()
    {
        return Arrays.asList(ConceptFeatureValueType.values());
    }

    private ConceptFeatureSupport getFeatureSupport()
    {
        return (ConceptFeatureSupport) featureSupportRegistry.getExtension(featureSupportId)
                .orElseThrow();
    }

    /**
     * Search for Entities in the current knowledge base based on a typed string. Use full text
     * search if it is available. Returns a sorted/ranked list of KBHandles
     */
    private List<KBHandle> listSearchResults(String aTypedString, ConceptFeatureValueType aType)
    {
        if (isBlank(aTypedString)) {
            return emptyList();
        }

        var kb = getSelectedKnowledgeBase();
        return conceptLinkingService.getLinkingInstancesInKBScope(
                kb != null ? kb.getRepositoryId() : null, null, aType, aTypedString, null, -1, null,
                feature.getObject().getProject());
    }
}
