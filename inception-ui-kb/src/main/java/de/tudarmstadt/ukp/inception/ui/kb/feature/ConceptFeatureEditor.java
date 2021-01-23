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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.wicket.event.Broadcast.BUBBLE;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.StringValue;
import org.danekja.java.util.function.serializable.SerializableFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.jquery.core.JQueryBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.event.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBindingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.conceptlinking.config.EntityLinkingProperties;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.ui.kb.IriInfoBadge;

/**
 * Component for editing knowledge-base-related features on annotations.
 */
public class ConceptFeatureEditor
    extends FeatureEditor
{
    private static final Logger LOG = LoggerFactory.getLogger(ConceptFeatureEditor.class);

    private static final long serialVersionUID = 7763348613632105600L;

    private AutoCompleteField focusComponent;
    private WebMarkupContainer descriptionContainer;
    private Label description;
    private IriInfoBadge iriBadge;
    private ExternalLink openIriLink;

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean ConceptLinkingService clService;
    private @SpringBean EntityLinkingProperties entityLinkingProperties;

    public ConceptFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel,
            IModel<AnnotatorState> aStateModel, AnnotationActionHandler aHandler)
    {
        super(aId, aItem, new CompoundPropertyModel<>(aModel));

        IModel<String> iriModel = LoadableDetachableModel.of(this::iriTooltipValue);

        iriBadge = new IriInfoBadge("iriInfoBadge", iriModel);
        iriBadge.setOutputMarkupPlaceholderTag(true);
        iriBadge.add(visibleWhen(() -> isNotBlank(iriBadge.getModelObject())));
        add(iriBadge);

        openIriLink = new ExternalLink("openIri", iriModel);
        openIriLink.setOutputMarkupPlaceholderTag(true);
        openIriLink.add(visibleWhen(() -> isNotBlank(iriBadge.getModelObject())));
        add(openIriLink);

        add(new DisabledKBWarning("disabledKBWarning", Model.of(getModelObject().feature)));

        add(focusComponent = new AutoCompleteField(MID_VALUE,
                _query -> getCandidates(aStateModel, aHandler, _query)));

        AnnotationFeature feat = getModelObject().feature;
        ConceptFeatureTraits traits = readFeatureTraits(feat);

        add(new KeyBindingsPanel("keyBindings", () -> traits.getKeyBindings(), aModel, aHandler)
                // The key bindings are only visible when the label is also enabled, i.e. when the
                // editor is used in a "normal" context and not e.g. in the keybindings
                // configuration panel
                .add(visibleWhen(() -> getLabelComponent().isVisible())));

        descriptionContainer = new WebMarkupContainer("descriptionContainer");
        descriptionContainer.setOutputMarkupPlaceholderTag(true);
        descriptionContainer.add(visibleWhen(
                () -> getLabelComponent().isVisible() && getModelObject().getValue() != null));
        add(descriptionContainer);

        description = new Label("description", LoadableDetachableModel.of(this::descriptionValue));
        descriptionContainer.add(description);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
    }

    private String descriptionValue()
    {
        return getModel().map(FeatureState::getValue)//
                .map(value -> (KBHandle) value)//
                .map(KBHandle::getDescription)//
                .map(value -> StringUtils.abbreviate(value, 130))//
                .orElse("no description")//
                .getObject();
    }

    private String iriTooltipValue()
    {
        return getModel().map(FeatureState::getValue)//
                .map(value -> (KBHandle) value)//
                .map(KBHandle::getIdentifier)//
                .orElse("")//
                .getObject();
    }

    private List<KBHandle> getCandidates(IModel<AnnotatorState> aStateModel,
            AnnotationActionHandler aHandler, String aInput)
    {
        if (aInput == null) {
            return emptyList();
        }

        String input = aInput;

        // Extract filter on the description
        final String descriptionFilter;
        if (input.contains("::")) {
            descriptionFilter = substringAfter(input, "::").trim();
            input = substringBefore(input, "::");
        }
        else {
            descriptionFilter = null;
        }

        // Extract exact match filter on the query
        boolean labelFilter = false;
        String trimmedInput = input.trim();
        if (trimmedInput.length() > 2 && trimmedInput.startsWith("\"")
                && trimmedInput.endsWith("\"")) {
            input = StringUtils.substring(trimmedInput, 1, -1).trim();
            labelFilter = true;
        }

        final String finalInput = input;

        List<KBHandle> choices;
        try {
            AnnotationFeature feat = getModelObject().feature;

            ConceptFeatureTraits traits = readFeatureTraits(feat);
            String repoId = traits.getRepositoryId();
            // Check if kb is actually enabled
            if (!(repoId == null || kbService.isKnowledgeBaseEnabled(feat.getProject(), repoId))) {
                return Collections.emptyList();
            }

            // If there is a selection, we try obtaining its text from the CAS and use it as an
            // additional item in the query. Note that there is not always a mention, e.g. when the
            // feature is used in a document-level annotations.
            CAS cas = aHandler != null ? aHandler.getEditorCas() : null;
            String mention = aStateModel != null ? aStateModel.getObject().getSelection().getText()
                    : null;
            int mentionBegin = aStateModel != null
                    ? aStateModel.getObject().getSelection().getBegin()
                    : -1;

            choices = clService.getLinkingInstancesInKBScope(traits.getRepositoryId(),
                    traits.getScope(), traits.getAllowedValueType(), finalInput, mention,
                    mentionBegin, cas, feat.getProject());
        }
        catch (Exception e) {
            choices = asList(new KBHandle("http://ERROR", "ERROR", e.getMessage(), "en"));
            error("An error occurred while retrieving entity candidates: " + e.getMessage());
            LOG.error("An error occurred while retrieving entity candidates", e);
            RequestCycle.get().find(IPartialPageRequestHandler.class)
                    .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));
        }

        if (labelFilter) {
            choices = choices.stream().filter(kb -> containsIgnoreCase(kb.getUiLabel(), finalInput))
                    .collect(Collectors.toList());
        }

        if (isNotBlank(descriptionFilter)) {
            choices = choices.stream()
                    .filter(kb -> containsIgnoreCase(kb.getDescription(), descriptionFilter))
                    .collect(Collectors.toList());
        }

        return choices.stream().limit(entityLinkingProperties.getCandidateDisplayLimit())
                .collect(Collectors.toList());
    }

    private ConceptFeatureTraits readFeatureTraits(AnnotationFeature aAnnotationFeature)
    {
        FeatureSupport<ConceptFeatureTraits> fs = featureSupportRegistry
                .getFeatureSupport(aAnnotationFeature);
        ConceptFeatureTraits traits = fs.readTraits(aAnnotationFeature);
        return traits;
    }

    @Override
    public void addFeatureUpdateBehavior()
    {
        focusComponent.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = -8944946839865527412L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes aAttributes)
            {
                super.updateAjaxAttributes(aAttributes);
                aAttributes.getDynamicExtraParameters()
                        .add(focusComponent.getIdentifierDynamicAttributeScript());
                addDelay(aAttributes, 250);
            }

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                aTarget.add(descriptionContainer, iriBadge, openIriLink);
                send(focusComponent, BUBBLE,
                        new FeatureEditorValueChangedEvent(ConceptFeatureEditor.this, aTarget));
            }
        });
    }

    @Override
    public FormComponent getFocusComponent()
    {
        return focusComponent;
    }

    /**
     * Special version of the {@link KnowledgeBaseItemAutoCompleteField} for used in the concept
     * feature editor.
     */
    public static class AutoCompleteField
        extends KnowledgeBaseItemAutoCompleteField
    {
        private static final long serialVersionUID = 5461442869971269291L;

        private IConverter<KBHandle> converter;
        private List<KBHandle> choiceCache;
        private boolean allowChoiceCache = false;

        public AutoCompleteField(String aId,
                SerializableFunction<String, List<KBHandle>> aChoiceProvider)
        {
            super(aId, aChoiceProvider);
            converter = newConverter();
        }

        @Override
        public void onConfigure(JQueryBehavior aBehavior)
        {
            super.onConfigure(aBehavior);

            // We need to explicitly trigger the change event on the input element in order to
            // trigger the Wicket AJAX update (if there is one). If we do not do this, then Kendo
            // will "forget" to trigger a change event if the label of the newly selected item is
            // the same as the label of the previously selected item!!!
            // Using the default select behavior of AutoCompleteTextField which is coupled to the
            // onSelected(AjaxRequestTarget aTarget) callback does unfortunatle not work well
            // because onSelected does not tell us when the auto-complete field is CLEARED!
            aBehavior.setOption("select", String.join(" ", //
                    "function (e) {", //
                    "  e.sender.element.trigger('change');", //
                    "}"));
        }

        @Override
        protected List<KBHandle> getChoices(String aInput)
        {
            if (!allowChoiceCache || choiceCache == null) {
                choiceCache = super.getChoices(aInput);
            }
            return choiceCache;
        }

        @Override
        public String[] getInputAsArray()
        {
            // If the web request includes the additional "identifier" parameter which is supposed
            // to contain the IRI of the selected item instead of its label, then we use that as the
            // value.
            WebRequest request = getWebRequest();
            IRequestParameters requestParameters = request.getRequestParameters();
            StringValue identifier = requestParameters
                    .getParameterValue(getInputName() + ":identifier");

            if (!identifier.isEmpty()) {
                return new String[] { identifier.toString() };
            }

            return super.getInputAsArray();
        }

        /**
         * When using this input component with an {@link AjaxFormChoiceComponentUpdatingBehavior},
         * it is necessary to request the identifier of the selected item as an additional dynamic
         * attribute, otherwise no distinction can be made between two items with the same label!
         */
        public String getIdentifierDynamicAttributeScript()
        {
            return String.join(" ", //
                    "var item = $(attrs.event.target).data('kendoAutoComplete').dataItem();", //
                    "if (item) {", //
                    "  return [{", //
                    "    'name': '" + getInputName() + ":identifier', ", //
                    "    'value': $(attrs.event.target).data('kendoAutoComplete').dataItem().identifier", //
                    "  }]", //
                    "}", //
                    "return [];");
        }

        @Override
        public <C> IConverter<C> getConverter(Class<C> aType)
        {
            if (aType != null && aType.isAssignableFrom(this.getType())) {
                return (IConverter<C>) converter;
            }

            return super.getConverter(aType);
        }

        private IConverter<KBHandle> newConverter()
        {
            return new IConverter<KBHandle>()
            {

                private static final long serialVersionUID = 1L;

                @Override
                public KBHandle convertToObject(String value, Locale locale)
                {
                    if (value == null) {
                        return null;
                    }

                    if (value.equals(getModelValue())) {
                        return getModelObject();
                    }

                    // Check choices only here since fetching choices can take some time. If we
                    // already have choices from a previous query, then we use them instead of
                    // reloading all the choices. This avoids having to load the choices when
                    // opening the dropdown AND when selecting one of the items from it.
                    List<KBHandle> choices;
                    try {
                        allowChoiceCache = true;
                        choices = getChoices(value);
                    }
                    finally {
                        allowChoiceCache = false;
                    }

                    if (choices.isEmpty()) {
                        return null;
                    }

                    // Check if we can find a match by the identifier. The identifier is unique
                    // while the same label may appear on multiple items
                    for (KBHandle handle : choices) {
                        if (value.equals(handle.getIdentifier())) {
                            return handle;
                        }
                    }

                    // @formatter:off
//                    // Check labels if there was no match on the identifier
//                    for (KBHandle handle : choices) {
//                        if (value.equals(getRenderer().getText(handle))) {
//                            return handle;
//                        }
//                    }
                    // @formatter:on

                    // If there was no match at all, return null
                    return null;
                }

                @Override
                public String convertToString(KBHandle value, Locale locale)
                {
                    return getRenderer().getText(value);
                }
            };
        }
    }
}
