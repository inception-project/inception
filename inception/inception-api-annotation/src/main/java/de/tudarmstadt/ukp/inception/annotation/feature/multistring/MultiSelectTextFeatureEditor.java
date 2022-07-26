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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.multiselect.lazy.MultiSelect;
import com.googlecode.wicket.kendo.ui.renderer.ChoiceRenderer;

import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.ConstraintsInUseIndicator;
import de.tudarmstadt.ukp.inception.annotation.feature.string.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupportProperties;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupportRegistry;

public class MultiSelectTextFeatureEditor
    extends FeatureEditor
{
    private static final long serialVersionUID = 7469241620229001983L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean StringFeatureSupportProperties properties;

    // // For showing the status of Constraints rules kicking in.
    // private RulesIndicator indicator = new RulesIndicator();

    private final MultiSelect<ReorderableTag> field;

    public MultiSelectTextFeatureEditor(String aId, MarkupContainer aOwner,
            final IModel<FeatureState> aFeatureStateModel, AnnotationActionHandler aHandler)
    {
        super(aId, aOwner, CompoundPropertyModel.of(aFeatureStateModel));

        field = new MultiSelect<ReorderableTag>("value", new ChoiceRenderer<>())
        {
            private static final long serialVersionUID = 7769511105678209462L;

            @Override
            protected List<ReorderableTag> getChoices(String aInput)
            {
                return MultiSelectTextFeatureEditor.this.getChoices(aFeatureStateModel, aInput);
            }

            @Override
            public void onConfigure(JQueryBehavior aBehavior)
            {
                super.onConfigure(aBehavior);

                aBehavior.setOption("autoWidth", true);
                aBehavior.setOption("animation", false);
                aBehavior.setOption("delay", 250);
                aBehavior.setOption("height", 300);
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
            @SuppressWarnings({ "unchecked", "rawtypes" })
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
        add(field);

        add(new ConstraintsInUseIndicator("textIndicator", getModel()));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public FormComponent getFocusComponent()
    {
        return field;
    }

    /**
     * Hides feature if "Hide un-constraint feature" is enabled and constraint rules are applied and
     * feature doesn't match any constraint rule
     */
    @Override
    public void onConfigure()
    {
        super.onConfigure();

        // if enabled and constraints rule execution returns anything other than green
        var featState = getModelObject();
        setVisible(!featState.feature.isHideUnconstraintFeature() || //
                (featState.indicator.isAffected()
                        && featState.indicator.getStatusColor().equals("green")));
    }

    private List<ReorderableTag> getChoices(final IModel<FeatureState> aFeatureStateModel,
            String aInput)
    {
        String input = aInput != null ? aInput.trim() : "";

        FeatureState featureState = aFeatureStateModel.getObject();
        TagSet tagset = featureState.getFeature().getTagset();
        List<ReorderableTag> choices = new ArrayList<>();

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
        for (String value : values) {
            if (!choices.stream().anyMatch(t -> t.getName().equals(value))) {
                choices.add(new ReorderableTag(value, false));
            }
        }

        if (!input.isEmpty()) {
            // Move any entries that match the input case-insensitive to the top
            List<ReorderableTag> inputMatchesInsensitive = choices.stream() //
                    .filter(t -> t.getName().equalsIgnoreCase(input)) //
                    .collect(toList());
            for (ReorderableTag t : inputMatchesInsensitive) {
                choices.remove(t);
                choices.add(0, t);
            }

            // Move any entries that match the input case-sensitive to the top
            List<ReorderableTag> inputMatchesSensitive = choices.stream() //
                    .filter(t -> t.getName().equals(input)) //
                    .collect(toList());
            if (inputMatchesSensitive.isEmpty()) {
                // If the input does not match any tagset entry, add a new virtual entry for
                // the
                // input so that we can select that and add it - this has no description.
                // If the input matches an existing entry, move it to the top.
                if (tagset == null || tagset.isCreateTag()) {
                    choices.add(0, new ReorderableTag(input, false));
                }
            }
            else {
                for (ReorderableTag t : inputMatchesSensitive) {
                    choices.remove(t);
                    choices.add(0, t);
                }
            }
        }

        return choices;
    }
}
