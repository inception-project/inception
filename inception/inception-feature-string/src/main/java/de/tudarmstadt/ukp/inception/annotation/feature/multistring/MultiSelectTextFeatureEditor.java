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
package de.tudarmstadt.ukp.inception.annotation.feature.multistring;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.template.IJQueryTemplate;
import org.wicketstuff.kendo.ui.form.multiselect.lazy.MultiSelect;
import org.wicketstuff.kendo.ui.renderer.ChoiceRenderer;

import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.ConstraintsInUseIndicator;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupportProperties;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.SuggestionStatePanel;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.support.kendo.KendoChoiceDescriptionScriptReference;

public class MultiSelectTextFeatureEditor
    extends FeatureEditor
{
    private static final String CID_TEXT_INDICATOR = "textIndicator";
    private static final String CID_VALUE = "value";

    private static final long serialVersionUID = 7469241620229001983L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean StringFeatureSupportProperties properties;

    // // For showing the status of Constraints rules kicking in.
    // private RulesIndicator indicator = new RulesIndicator();

    private FormComponent<?> field;
    private boolean featureUpdateBehaviorRequested = false;
    private boolean featureUpdateBehaviorAdded = false;

    public MultiSelectTextFeatureEditor(String aId, MarkupContainer aOwner,
            final IModel<FeatureState> aModel, AnnotationActionHandler aHandler)
    {
        super(aId, aOwner, CompoundPropertyModel.of(aModel));

        field = createInput();
        add(field);

        add(new ConstraintsInUseIndicator(CID_TEXT_INDICATOR, getModel()));

        add(new SuggestionStatePanel("suggestionInfo", aModel));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onConfigure()
    {
        super.onConfigure();

        // Workaround for https://github.com/sebfz1/wicket-jquery-ui/issues/352
        if ((isEnabledInHierarchy() && !(field instanceof MultiSelect))
                || !isEnabledInHierarchy() && (field instanceof MultiSelect)) {
            field = (FormComponent<ReorderableTag>) field.replaceWith(createInput());
        }

        if (featureUpdateBehaviorRequested && !featureUpdateBehaviorAdded) {
            super.addFeatureUpdateBehavior();
            featureUpdateBehaviorAdded = true;
        }

        // Hides feature if "Hide un-constraint feature" is enabled and constraint rules are applied
        // and feature doesn't match any constraint rule
        // if enabled and constraints rule execution returns anything other than green
        var featState = getModelObject();
        setVisible(!featState.feature.isHideUnconstraintFeature() || //
                (featState.indicator.isAffected()
                        && featState.indicator.getStatusColor().equals("green")));
    }

    private FormComponent<?> createInput()
    {
        featureUpdateBehaviorAdded = false;
        if (isEnabledInHierarchy()) {
            return createEditableInput();
        }
        else {
            return createReadOnlyInput();
        }
    }

    @Override
    public void addFeatureUpdateBehavior()
    {
        featureUpdateBehaviorRequested = true;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private FormComponent<String> createReadOnlyInput()
    {
        var input = new org.wicketstuff.kendo.ui.form.multiselect. //
                MultiSelect<String>(CID_VALUE)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void renderHead(IHeaderResponse aResponse)
            {
                super.renderHead(aResponse);

                aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
            }

            @Override
            public void onConfigure(JQueryBehavior aBehavior)
            {
                super.onConfigure(aBehavior);
                styleMultiSelect(aBehavior);
            }
        };
        var choices = getChoices(getModel(), null).stream().map(t -> t.getName()).collect(toList());
        input.setChoices(Model.ofList(choices));
        return (FormComponent) input;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private FormComponent<ReorderableTag> createEditableInput()
    {
        return (FormComponent) new MultiSelect<ReorderableTag>(CID_VALUE, new ChoiceRenderer<>())
        {
            private static final long serialVersionUID = 7769511105678209462L;

            @Override
            public void renderHead(IHeaderResponse aResponse)
            {
                super.renderHead(aResponse);

                aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
            }

            @Override
            protected List<ReorderableTag> getChoices(String aInput)
            {
                return MultiSelectTextFeatureEditor.this
                        .getChoices(MultiSelectTextFeatureEditor.this.getModel(), aInput);
            }

            @Override
            public void onConfigure(JQueryBehavior aBehavior)
            {
                super.onConfigure(aBehavior);
                styleMultiSelect(aBehavior);
            }

            @Override
            protected IJQueryTemplate newTemplate()
            {
                return KendoChoiceDescriptionScriptReference.templateReorderable();
            }

            /*
             * Below is a hack which is required because all the text feature editors are expected
             * to write a plain string into the feature state. However, we cannot have an {@code
             * MultiSelectTextFeatureEditor<String>} field because then we would loose easy access
             * to the tag description which we show in the tooltips. So we hack the converter to
             * return strings on the way out into the model. This is a very evil hack and we need to
             * avoid declaring generic types because we work against them!
             */
            @Override
            public void convertInput()
            {
                super.convertInput();
                setConvertedInput((List) getConvertedInput().stream() //
                        .map(ReorderableTag::getName) //
                        .distinct() //
                        .collect(toList()));
            }
        };
    }

    private void styleMultiSelect(JQueryBehavior aBehavior)
    {
        aBehavior.setOption("autoWidth", true);
        aBehavior.setOption("animation", false);
        aBehavior.setOption("delay", 250);
        aBehavior.setOption("height", 300);
        aBehavior.setOption("open", KendoChoiceDescriptionScriptReference.applyTooltipScript());
        aBehavior.setOption("dataBound",
                KendoChoiceDescriptionScriptReference.applyTooltipScript());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public FormComponent getFocusComponent()
    {
        return field;
    }

    private List<ReorderableTag> getChoices(final IModel<FeatureState> aFeatureStateModel,
            String aInput)
    {
        var input = aInput != null ? aInput.trim() : "";

        var featureState = aFeatureStateModel.getObject();
        var tagset = featureState.getFeature().getTagset();
        var choices = new ArrayList<ReorderableTag>();

        Collection<String> values;
        if (featureState.getValue() instanceof Collection) {
            values = (Collection<String>) featureState.getValue();
        }
        else {
            values = emptyList();
        }

        // If there is a tagset, add it to the choices - this may include descriptions
        if (tagset != null) {
            featureState.tagset.stream() //
                    .filter(t -> input.isEmpty() //
                            || startsWithIgnoreCase(t.getName(), input)
                            || values.contains(t.getName())) //
                    .limit(properties.getAutoCompleteMaxResults()) //
                    .forEach(choices::add);
        }

        // If the currently selected values contain any values not covered by the tagset,
        // add virtual entries for them as well
        for (var value : values) {
            if (!choices.stream().anyMatch(t -> t.getName().equals(value))) {
                choices.add(new ReorderableTag(value, false));
            }
        }

        if (!input.isEmpty()) {
            // Move any entries that match the input case-insensitive to the top
            var inputMatchesInsensitive = choices.stream() //
                    .filter(t -> t.getName().equalsIgnoreCase(input)) //
                    .collect(toList());
            for (var t : inputMatchesInsensitive) {
                choices.remove(t);
                choices.add(0, t);
            }

            // Move any entries that match the input case-sensitive to the top
            var inputMatchesSensitive = choices.stream() //
                    .filter(t -> t.getName().equals(input)) //
                    .collect(toList());
            if (inputMatchesSensitive.isEmpty()) {
                // If the input does not match any tagset entry, add a new virtual entry for
                // the input so that we can select that and add it - this has no description.
                // If the input matches an existing entry, move it to the top.
                if (tagset == null || tagset.isCreateTag()) {
                    choices.add(0, new ReorderableTag(input, false));
                }
            }
            else {
                for (var t : inputMatchesSensitive) {
                    choices.remove(t);
                    choices.add(0, t);
                }
            }
        }

        return choices;
    }
}
