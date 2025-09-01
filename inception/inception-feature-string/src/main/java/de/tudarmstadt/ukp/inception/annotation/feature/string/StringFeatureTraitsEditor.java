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
package de.tudarmstadt.ukp.inception.annotation.feature.string;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.util.Arrays;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.form.NumberTextField;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBindingsConfigurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.UimaPrimitiveFeatureSupport_ImplBase;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.RecommendableFeatureTrait;
import de.tudarmstadt.ukp.inception.schema.api.feature.RetainSuggestionInfoPanel;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class StringFeatureTraitsEditor
    extends GenericPanel<AnnotationFeature>
{
    private static final long serialVersionUID = -9082045435380184514L;

    private static final String MID_FORM = "form";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    private String featureSupportId;
    private IModel<AnnotationFeature> feature;
    private CompoundPropertyModel<StringFeatureTraits> traits;
    private KeyBindingsConfigurationPanel keyBindings;

    public StringFeatureTraitsEditor(String aId,
            UimaPrimitiveFeatureSupport_ImplBase<StringFeatureTraits> aFS,
            IModel<AnnotationFeature> aFeature)
    {
        super(aId, aFeature);

        featureSupportId = aFS.getId();
        feature = aFeature;

        traits = CompoundPropertyModel.of(getFeatureSupport().readTraits(feature.getObject()));

        keyBindings = newKeyBindingsConfigurationPanel(aFeature);
        add(keyBindings);

        var form = new Form<StringFeatureTraits>(MID_FORM, traits)
        {
            private static final long serialVersionUID = -3109239605783291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();

                // Tagsets only are allowed for single-row input fields, not for textareas
                if (traits.getObject().isMultipleRows()) {
                    feature.getObject().setTagset(null);
                }
                else {
                    traits.getObject().setDynamicSize(false);
                }

                getFeatureSupport().writeTraits(feature.getObject(), traits.getObject());
            }
        };
        form.setOutputMarkupPlaceholderTag(true);
        add(form);

        var defaultValue = new TextField<>("defaultValue");
        queue(defaultValue);

        var collapsedRows = new NumberTextField<Integer>("collapsedRows", Integer.class,
                new Options().set("format", "'#'"));
        collapsedRows.setModel(PropertyModel.of(traits, "collapsedRows"));
        collapsedRows.setMinimum(1);
        collapsedRows.setMaximum(100);
        collapsedRows.add(visibleWhen(
                () -> traits.getObject().isMultipleRows() && !traits.getObject().isDynamicSize()));
        form.add(collapsedRows);

        var expandedRows = new NumberTextField<Integer>("expandedRows", Integer.class,
                new Options().set("format", "'#'"));
        expandedRows.setModel(PropertyModel.of(traits, "expandedRows"));
        expandedRows.setMinimum(1);
        expandedRows.setMaximum(100);
        expandedRows.add(visibleWhen(
                () -> traits.getObject().isMultipleRows() && !traits.getObject().isDynamicSize()));
        form.add(expandedRows);

        var tagset = new DropDownChoice<TagSet>("tagset");
        tagset.setOutputMarkupPlaceholderTag(true);
        tagset.setChoiceRenderer(new ChoiceRenderer<>("name"));
        tagset.setNullValid(true);
        tagset.setModel(PropertyModel.of(aFeature, "tagset"));
        tagset.setChoices(LoadableDetachableModel
                .of(() -> annotationService.listTagSets(aFeature.getObject().getProject())));
        tagset.add(visibleWhen(() -> !traits.getObject().isMultipleRows()));
        // If we change the tagset, the input component for the key bindings may change, so we need
        // to re-generate the key bindings
        tagset.add(new LambdaAjaxFormComponentUpdatingBehavior("change", _target -> {
            _target.add(form);
            refreshKeyBindings(_target, aFeature);
        }));
        form.add(tagset);

        var multipleRows = new CheckBox("multipleRows");
        multipleRows.setOutputMarkupId(true);
        multipleRows.setModel(PropertyModel.of(traits, "multipleRows"));
        multipleRows.add(new LambdaAjaxFormComponentUpdatingBehavior("change", _target -> {
            if (multipleRows.getModelObject()) {
                aFeature.getObject().setTagset(null);
            }
            _target.add(form);
            refreshKeyBindings(_target, aFeature);
        }));
        form.add(multipleRows);

        var dynamicSize = new CheckBox("dynamicSize");
        dynamicSize.setOutputMarkupId(true);
        dynamicSize.setModel(PropertyModel.of(traits, "dynamicSize"));
        dynamicSize.add(
                new LambdaAjaxFormComponentUpdatingBehavior("change", target -> target.add(form)));
        dynamicSize.add(visibleWhen(() -> traits.getObject().isMultipleRows()));
        form.add(dynamicSize);

        var editorTypeContainer = new WebMarkupContainer("editorTypeContainer");
        editorTypeContainer.setOutputMarkupPlaceholderTag(true);
        editorTypeContainer.add(visibleWhen(() -> !traits.getObject().isMultipleRows()
                && aFeature.getObject().getTagset() != null));
        form.add(editorTypeContainer);

        var editorType = new DropDownChoice<StringFeatureTraits.EditorType>("editorType");
        editorType.setModel(PropertyModel.of(traits, "editorType"));
        editorType.setChoices(Arrays.asList(StringFeatureTraits.EditorType.values()));
        editorType.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        editorTypeContainer.add(editorType);

        form.add(new RetainSuggestionInfoPanel("retainSuggestionInfo", aFeature,
                traits.map(RecommendableFeatureTrait.class::cast)));
    }

    private void refreshKeyBindings(AjaxRequestTarget aTarget, IModel<AnnotationFeature> aFeature)
    {
        var newKeyBindings = newKeyBindingsConfigurationPanel(aFeature);
        keyBindings.replaceWith(newKeyBindings);
        keyBindings = newKeyBindings;
        aTarget.add(keyBindings);
    }

    private KeyBindingsConfigurationPanel newKeyBindingsConfigurationPanel(
            IModel<AnnotationFeature> aFeature)
    {
        getFeatureSupport().writeTraits(aFeature.getObject(), traits.getObject());
        var panel = new KeyBindingsConfigurationPanel("keyBindings", aFeature,
                traits.bind("keyBindings"));
        panel.setOutputMarkupId(true);
        // panel.add(visibleWhen(() -> !traits.getObject().isMultipleRows()));
        return panel;
    }

    @SuppressWarnings("unchecked")
    private UimaPrimitiveFeatureSupport_ImplBase<StringFeatureTraits> getFeatureSupport()
    {
        return (UimaPrimitiveFeatureSupport_ImplBase<StringFeatureTraits>) featureSupportRegistry
                .getExtension(featureSupportId).orElseThrow();
    }
}
