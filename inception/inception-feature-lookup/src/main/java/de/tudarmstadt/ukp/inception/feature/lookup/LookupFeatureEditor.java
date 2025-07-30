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
package de.tudarmstadt.ukp.inception.feature.lookup;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static org.apache.wicket.event.Broadcast.BUBBLE;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
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
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.StringValue;
import org.danekja.java.util.function.serializable.SerializableFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.jquery.core.renderer.TextRenderer;
import org.wicketstuff.jquery.core.template.IJQueryTemplate;
import org.wicketstuff.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.kendo.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.inception.support.kendo.KendoStyleUtils;

public class LookupFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = 7763348613632105600L;

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean LookupService lookupService;

    private AutoCompleteField focusComponent;
    private WebMarkupContainer descriptionContainer;
    private Label description;

    public LookupFeatureEditor(String aId, MarkupContainer aOwner, IModel<FeatureState> aModel,
            IModel<AnnotatorState> aStateModel, AnnotationActionHandler aHandler)
    {
        super(aId, aOwner, new CompoundPropertyModel<>(aModel));

        AnnotationFeature feat = getModelObject().feature;
        LookupFeatureTraits traits = readFeatureTraits(feat);

        descriptionContainer = new WebMarkupContainer("descriptionContainer");
        descriptionContainer.setOutputMarkupPlaceholderTag(true);
        descriptionContainer.add(visibleWhen(
                () -> getLabelComponent().isVisible() && getModelObject().getValue() != null));
        add(descriptionContainer);

        description = new Label("description", LoadableDetachableModel.of(this::descriptionValue));
        descriptionContainer.add(description);

        add(focusComponent = new AutoCompleteField(MID_VALUE, _query -> {
            try {
                // If there is a selection, we try obtaining its text from the CAS and use it as an
                // additional item in the query. Note that there is not always a mention, e.g. when
                // the feature is used in a document-level annotations.
                String mention = aStateModel != null
                        ? aStateModel.getObject().getSelection().getText()
                        : null;

                return lookupService.query(traits, _query, mention);
            }
            catch (Exception e) {
                error("An error occurred while retrieving entity candidates: " + e.getMessage());
                LOG.error("An error occurred while retrieving entity candidates", e);
                RequestCycle.get().find(IPartialPageRequestHandler.class)
                        .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));
                return asList(new LookupEntry("http://ERROR", "ERROR", e.getMessage()));
            }
        }));
    }

    private String descriptionValue()
    {
        return getModel().map(FeatureState::getValue)//
                .map(value -> (LookupEntry) value)//
                .map(LookupEntry::getDescription)//
                .map(value -> StringUtils.abbreviate(value, 130))//
                .orElse("no description")//
                .getObject();
    }

    @OnEvent
    public void onFeatureEditorValueChanged(FeatureEditorValueChangedEvent aEvent)
    {
        aEvent.getTarget().add(descriptionContainer);
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
                        new FeatureEditorValueChangedEvent(LookupFeatureEditor.this, aTarget));
            }
        });
    }

    protected LookupFeatureTraits readFeatureTraits(AnnotationFeature aAnnotationFeature)
    {
        FeatureSupport<?> fs = featureSupportRegistry.findExtension(aAnnotationFeature)
                .orElseThrow();
        return (LookupFeatureTraits) fs.readTraits(aAnnotationFeature);
    }

    @Override
    public FormComponent<LookupEntry> getFocusComponent()
    {
        return focusComponent;
    }

    public static class AutoCompleteField
        extends AutoCompleteTextField<LookupEntry>
    {
        private static final long serialVersionUID = 5461442869971269291L;

        private IConverter<LookupEntry> converter;
        private List<LookupEntry> choiceCache;
        private boolean allowChoiceCache = false;
        private SerializableFunction<String, List<LookupEntry>> choiceProvider;

        public AutoCompleteField(String aId,
                SerializableFunction<String, List<LookupEntry>> aChoiceProvider)
        {
            super(aId, new TextRenderer<LookupEntry>("uiLabel"));
            converter = newConverter();
            choiceProvider = aChoiceProvider;
        }

        @Override
        public void onConfigure(JQueryBehavior aBehavior)
        {
            super.onConfigure(aBehavior);

            aBehavior.setOption("ignoreCase", false);
            aBehavior.setOption("delay", 500);
            aBehavior.setOption("animation", false);
            aBehavior.setOption("footerTemplate",
                    Options.asString("#: instance.dataSource.total() # items found"));

            KendoStyleUtils.autoDropdownHeight(aBehavior);
            KendoStyleUtils.autoDropdownWidth(aBehavior);
            KendoStyleUtils.resetDropdownSelectionOnOpen(aBehavior);
            KendoStyleUtils.keepDropdownVisibleWhenScrolling(aBehavior);

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
        protected List<LookupEntry> getChoices(String aInput)
        {
            if (!allowChoiceCache || choiceCache == null) {
                choiceCache = choiceProvider.apply(aInput);
            }
            return choiceCache;
        }

        @Override
        public String[] getInputAsArray()
        {
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

        private IConverter<LookupEntry> newConverter()
        {
            return new IConverter<LookupEntry>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public LookupEntry convertToObject(String value, Locale locale)
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
                    List<LookupEntry> choices;
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
                    for (LookupEntry handle : choices) {
                        if (value.equals(handle.getIdentifier())) {
                            return handle;
                        }
                    }

                    // If there was no match at all, return null
                    return null;
                }

                @Override
                public String convertToString(LookupEntry value, Locale locale)
                {
                    return getRenderer().getText(value);
                }
            };
        }

        @Override
        protected IJQueryTemplate newTemplate()
        {
            return new LookupEntryTemplate();
        }
    }
}
