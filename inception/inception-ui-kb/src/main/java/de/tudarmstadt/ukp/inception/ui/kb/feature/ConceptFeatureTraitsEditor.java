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
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.LambdaChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBinding;
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
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

/**
 * Component for editing the traits of knowledge-base-related features in the feature detail editor
 * of the project settings.
 */
public class ConceptFeatureTraitsEditor
    extends Panel
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
    private CompoundPropertyModel<Traits> traits;

    public ConceptFeatureTraitsEditor(String aId, ConceptFeatureSupport aFS,
            IModel<AnnotationFeature> aFeatureModel)
    {
        super(aId, aFeatureModel);

        // We cannot retain a reference to the actual ConceptFeatureSupport instance because that
        // is not serializable, but we can retain its ID and look it up again from the registry
        // when required.
        featureSupportId = aFS.getId();
        feature = aFeatureModel;
        traits = CompoundPropertyModel.of(readTraits());

        var form = new Form<Traits>(MID_FORM, traits)
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
                _query -> listSearchResults(_query, CONCEPT)).setOutputMarkupPlaceholderTag(true));

        form.add(new DropDownChoice<>(MID_KNOWLEDGE_BASE,
                LoadableDetachableModel.of(this::listKnowledgeBases),
                new LambdaChoiceRenderer<>(KnowledgeBase::getName)).setNullValid(true)
                        .add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::refresh)));
        form.add(new DropDownChoice<>(MID_ALLOWED_VALUE_TYPE,
                LoadableDetachableModel.of(this::listAllowedTypes))
                        .add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::refresh)));

        form.add(new DisabledKBWarning("disabledKBWarning", feature,
                traits.bind("knowledgeBase.repositoryId")));
        add(form);

        var retainSuggestionInfo = new CheckBox("retainSuggestionInfo");
        retainSuggestionInfo.setOutputMarkupId(true);
        retainSuggestionInfo.setModel(PropertyModel.of(traits, "retainSuggestionInfo"));
        form.add(retainSuggestionInfo);

        add(new KeyBindingsConfigurationPanel(MID_KEY_BINDINGS, aFeatureModel,
                traits.bind(MID_KEY_BINDINGS)).setOutputMarkupId(true));

    }

    private void refresh(AjaxRequestTarget aTarget)
    {
        var t = traits.getObject();
        t.scope = loadConcept(t.knowledgeBase, t.scope != null ? t.scope.getIdentifier() : null);
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

    /**
     * Read traits and then transfer the values from the actual traits model
     * {{@link ConceptFeatureTraits}} to the the UI traits model ({@link Traits}).
     */
    private Traits readTraits()
    {
        var project = feature.getObject().getProject();

        var result = new Traits();

        var t = getFeatureSupport().readTraits(feature.getObject());

        if (t.getRepositoryId() != null) {
            kbService.getKnowledgeBaseById(project, t.getRepositoryId())
                    .ifPresent(kb -> result.knowledgeBase = kb);
        }

        if (t.getAllowedValueType() != null) {
            result.allowedValueType = t.getAllowedValueType();
        }
        else {
            // Allow all values as default
            result.allowedValueType = ConceptFeatureValueType.ANY_OBJECT;
        }

        result.scope = loadConcept(result.knowledgeBase, t.getScope());
        result.keyBindings = t.getKeyBindings();
        result.retainSuggestionInfo = t.isRetainSuggestionInfo();

        return result;
    }

    /**
     * Transfer the values from the UI traits model ({@link Traits}) to the actual traits model
     * {{@link ConceptFeatureTraits}} and then store them.
     */
    private void writeTraits()
    {
        var t = new ConceptFeatureTraits();
        if (traits.getObject().knowledgeBase != null) {
            t.setRepositoryId(traits.getObject().knowledgeBase.getRepositoryId());

        }

        if (traits.getObject().scope != null) {
            t.setScope(traits.getObject().scope.getIdentifier());
        }

        t.setAllowedValueType(traits.getObject().allowedValueType);
        t.setKeyBindings(traits.getObject().keyBindings);
        t.setRetainSuggestionInfo(traits.getObject().retainSuggestionInfo);

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

        var t = traits.getObject();
        return conceptLinkingService.getLinkingInstancesInKBScope(
                t.knowledgeBase != null ? t.knowledgeBase.getRepositoryId() : null, null, aType,
                aTypedString, null, -1, null, feature.getObject().getProject());
    }

    /**
     * A UI model holding the traits while the user is editing them. They are read/written to the
     * actual {@link ConceptFeatureTraits} via {@link ConceptFeatureTraitsEditor#readTraits()} and
     * {@link ConceptFeatureTraitsEditor#writeTraits()}.
     */
    @SuppressWarnings("unused")
    private static class Traits
        implements Serializable
    {
        private static final long serialVersionUID = 7746975883022031685L;

        KnowledgeBase knowledgeBase;
        KBHandle scope;
        ConceptFeatureValueType allowedValueType;
        List<KeyBinding> keyBindings;
        boolean retainSuggestionInfo = false;
    }
}
