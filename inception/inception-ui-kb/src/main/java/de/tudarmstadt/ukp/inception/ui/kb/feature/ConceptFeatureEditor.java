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

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.wicket.event.Broadcast.BUBBLE;
import static org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog.CONTENT_ID;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.validation.validator.UrlValidator;
import org.danekja.java.util.function.serializable.SerializableFunction;
import org.wicketstuff.event.annotation.OnEvent;
import org.wicketstuff.jquery.core.JQueryBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBindingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.SuggestionStatePanel;
import de.tudarmstadt.ukp.inception.support.kendo.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.ui.kb.IriInfoBadge;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxInstanceSelectionEvent;

/**
 * Component for editing knowledge-base-related features on annotations.
 */
public class ConceptFeatureEditor
    extends ConceptFeatureEditor_ImplBase
{
    private static final long serialVersionUID = 7763348613632105600L;

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean KnowledgeBaseService knowledgeBaseService;

    private AutoCompleteField focusComponent;
    private WebMarkupContainer deprecationMarker;
    private WebMarkupContainer descriptionContainer;
    private Label description;
    private IriInfoBadge iriBadge;
    private ExternalLink openIriLink;
    private UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" });
    private BootstrapModalDialog dialog;

    public ConceptFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel,
            IModel<AnnotatorState> aStateModel, AnnotationActionHandler aHandler)
    {
        super(aId, aItem, aModel);

        var feat = getModelObject().feature;
        var traits = readFeatureTraits(feat);

        var iriModel = LoadableDetachableModel.of(this::iriTooltipValue);

        iriBadge = new IriInfoBadge("iriInfoBadge", iriModel);
        iriBadge.setOutputMarkupPlaceholderTag(true);
        iriBadge.add(visibleWhen(iriBadge.getModel().map(urlValidator::isValid)));
        add(iriBadge);

        openIriLink = new ExternalLink("openIri", iriModel);
        openIriLink.setOutputMarkupPlaceholderTag(true);
        openIriLink.add(visibleWhen(() -> isNotBlank(iriBadge.getModelObject())));
        add(openIriLink);

        deprecationMarker = new WebMarkupContainer("deprecationMarker");
        deprecationMarker.setOutputMarkupPlaceholderTag(true);
        deprecationMarker.add(visibleWhen(this::isDeprecated));
        add(deprecationMarker);

        descriptionContainer = new WebMarkupContainer("descriptionContainer");
        descriptionContainer.add(visibleWhen(
                () -> getLabelComponent().isVisible() && getModelObject().getValue() != null));
        add(descriptionContainer);

        description = new Label("description", LoadableDetachableModel.of(this::descriptionValue));
        descriptionContainer.add(description);

        add(focusComponent = new AutoCompleteField(MID_VALUE,
                _query -> getCandidates(aStateModel, aHandler, _query)));

        add(new SuggestionStatePanel("suggestionInfo", aModel));

        add(new KeyBindingsPanel("keyBindings", () -> traits.getKeyBindings(), aModel, aHandler)
                // The key bindings are only visible when the label is also enabled, i.e. when the
                // editor is used in a "normal" context and not e.g. in the keybindings
                // configuration panel
                .add(visibleWhen(() -> getLabelComponent().isVisible())));

        dialog = new BootstrapModalDialog("dialog");
        dialog.trapFocus();
        queue(dialog);

        queue(new LambdaAjaxLink("openBrowseDialog", this::actionOpenBrowseDialog)
                .add(LambdaBehavior.visibleWhen(this::isBrowsingAllowed)));
    }

    private boolean isBrowsingAllowed()
    {
        var traits = featureSupportRegistry.readTraits(getModelObject().feature,
                ConceptFeatureTraits::new);

        // There is now KB selector in the browser yet, so we do not show it unless either the
        // feature is bound to a specific KB or there is only a single KB in the project.
        if (traits.getRepositoryId() == null && knowledgeBaseService
                .hasMoreThanOneEnabledKnowledgeBases(getModelObject().feature.getProject())) {
            return false;
        }

        // Properties are not supported in the browser
        if (traits.getAllowedValueType() == ConceptFeatureValueType.PROPERTY) {
            return false;
        }

        return true;
    }

    private void actionOpenBrowseDialog(AjaxRequestTarget aTarget)
    {
        var traits = featureSupportRegistry.readTraits(getModelObject().feature,
                ConceptFeatureTraits::new);

        var content = new BrowseKnowledgeBaseDialogContentPanel(CONTENT_ID,
                getModel().map(fs -> fs.getFeature().getProject()),
                getModel().map(fs -> (KBObject) fs.value), Model.of(traits));
        dialog.open(content, aTarget);
    }

    private boolean isDeprecated()
    {
        return getModel().map(FeatureState::getValue)//
                .map(value -> (KBHandle) value)//
                .map(KBHandle::isDeprecated) //
                .orElse(false)//
                .getObject();

    }

    private String descriptionValue()
    {
        return getModel().map(FeatureState::getValue) //
                .map(value -> (KBHandle) value) //
                .map(KBHandle::getDescription) //
                .map(value -> StringUtils.abbreviate(value, 1000)) //
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

    @OnEvent
    public void onFeatureEditorValueChanged(FeatureEditorValueChangedEvent aEvent)
    {
        aEvent.getTarget().add(this);
    }

    @OnEvent
    public void onConceptSelectionEvent(AjaxConceptSelectionEvent aEvent)
    {
        selectItem(aEvent.getTarget(), aEvent.getSelection());
    }

    @OnEvent
    public void onInstanceSelectionEvent(AjaxInstanceSelectionEvent aEvent)
    {
        selectItem(aEvent.getTarget(), aEvent.getSelection());
    }

    private void selectItem(AjaxRequestTarget aTarget, KBObject aKBObject)
    {
        getModelObject().value = aKBObject;
        dialog.close(aTarget);
        send(focusComponent, BUBBLE, new FeatureEditorValueChangedEvent(this, aTarget));
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
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
            }

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                send(focusComponent, BUBBLE,
                        new FeatureEditorValueChangedEvent(ConceptFeatureEditor.this, aTarget));
            }
        });
    }

    @Override
    protected ConceptFeatureTraits readFeatureTraits(AnnotationFeature aAnnotationFeature)
    {
        FeatureSupport<?> fs = featureSupportRegistry.findExtension(aAnnotationFeature)
                .orElseThrow();
        return (ConceptFeatureTraits) fs.readTraits(aAnnotationFeature);
    }

    @Override
    public FormComponent<KBHandle> getFocusComponent()
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
            // onSelected(AjaxRequestTarget aTarget) callback does unfortunately not work well
            // because onSelected does not tell us when the auto-complete field is CLEARED!
            aBehavior.setOption("select", String.join(" ", //
                    "function (e) {", //
                    "  e.sender.select(e.item);", //
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
         * 
         * @return JavaScript snippet
         */
        public String getIdentifierDynamicAttributeScript()
        {
            return String.join(" ", //
                    "var item = $(attrs.event.target).data('kendoAutoComplete')?.dataItem();", //
                    "if (item) {", //
                    "  return [{", //
                    "    'name': '" + getInputName() + ":identifier', ", //
                    "    'value': item.identifier", //
                    "  }]", //
                    "}", //
                    "return [];");
        }

        @SuppressWarnings("unchecked")
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
