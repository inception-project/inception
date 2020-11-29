/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.kendo.ui.form.NumberTextField;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.UimaPrimitiveFeatureSupport_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBindingsConfigurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class StringFeatureTraitsEditor
    extends Panel
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

        // We cannot retain a reference to the actual SlotFeatureSupport instance because that
        // is not serializable, but we can retain its ID and look it up again from the registry
        // when required.
        featureSupportId = aFS.getId();
        feature = aFeature;

        traits = CompoundPropertyModel.of(getFeatureSupport().readTraits(feature.getObject()));

        keyBindings = newKeyBindingsConfigurationPanel(aFeature);
        add(keyBindings);

        Form<StringFeatureTraits> form = new Form<StringFeatureTraits>(MID_FORM, traits)
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

        NumberTextField<Integer> collapsedRows = new NumberTextField<>("collapsedRows",
                Integer.class);
        collapsedRows.setModel(PropertyModel.of(traits, "collapsedRows"));
        collapsedRows.setMinimum(1);
        collapsedRows.add(visibleWhen(
                () -> traits.getObject().isMultipleRows() && !traits.getObject().isDynamicSize()));
        form.add(collapsedRows);

        NumberTextField<Integer> expandedRows = new NumberTextField<>("expandedRows",
                Integer.class);
        expandedRows.setModel(PropertyModel.of(traits, "expandedRows"));
        expandedRows.setMinimum(1);
        expandedRows.add(visibleWhen(
                () -> traits.getObject().isMultipleRows() && !traits.getObject().isDynamicSize()));
        form.add(expandedRows);

        DropDownChoice<TagSet> tagset = new BootstrapSelect<>("tagset");
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
            KeyBindingsConfigurationPanel newKeyBindings = newKeyBindingsConfigurationPanel(
                    aFeature);
            keyBindings.replaceWith(newKeyBindings);
            keyBindings = newKeyBindings;
            _target.add(keyBindings);
        }));
        form.add(tagset);

        CheckBox multipleRows = new CheckBox("multipleRows");
        multipleRows.setModel(PropertyModel.of(traits, "multipleRows"));
        multipleRows.add(
                new LambdaAjaxFormComponentUpdatingBehavior("change", target -> target.add(form)));
        form.add(multipleRows);

        CheckBox dynamicSize = new CheckBox("dynamicSize");
        dynamicSize.setModel(PropertyModel.of(traits, "dynamicSize"));
        dynamicSize.add(
                new LambdaAjaxFormComponentUpdatingBehavior("change", target -> target.add(form)));
        dynamicSize.add(visibleWhen(() -> traits.getObject().isMultipleRows()));
        form.add(dynamicSize);
    }

    private KeyBindingsConfigurationPanel newKeyBindingsConfigurationPanel(
            IModel<AnnotationFeature> aFeature)
    {
        KeyBindingsConfigurationPanel keyBindings = new KeyBindingsConfigurationPanel("keyBindings",
                aFeature, traits.bind("keyBindings"));
        keyBindings.setOutputMarkupId(true);
        return keyBindings;
    }

    private UimaPrimitiveFeatureSupport_ImplBase<StringFeatureTraits> getFeatureSupport()
    {
        return featureSupportRegistry.getFeatureSupport(featureSupportId);
    }
}
