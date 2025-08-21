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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxSubmitLink;

/**
 * Can be added to a feature support traits editor to configure key bindings.
 */
public class KeyBindingsConfigurationPanel
    extends Panel
{
    private static final long serialVersionUID = -8294428032177255299L;

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean AnnotationSchemaService schemaService;

    private final WebMarkupContainer keyBindingsContainer;
    private final IModel<List<KeyBinding>> keyBindings;

    private final FeatureEditor editor;
    private final IModel<FeatureState> featureState;

    public KeyBindingsConfigurationPanel(String aId, IModel<AnnotationFeature> aModel,
            IModel<List<KeyBinding>> aKeyBindings)
    {
        super(aId, aModel);

        keyBindings = aKeyBindings;

        var keyBindingForm = new Form<KeyBinding>("keyBindingForm",
                CompoundPropertyModel.of(new KeyBinding()));
        add(keyBindingForm);

        keyBindingsContainer = new WebMarkupContainer("keyBindingsContainer");
        keyBindingsContainer.setOutputMarkupPlaceholderTag(true);
        keyBindingForm.add(keyBindingsContainer);

        // We cannot make the key-combo field a required one here because then we'd get a message
        // about keyCombo not being set when saving the entire feature details form!
        keyBindingsContainer.add(new TextField<String>("keyCombo").add(new KeyComboValidator()));
        keyBindingsContainer.add(new LambdaAjaxSubmitLink<>("addKeyBinding", this::addKeyBinding));

        var feature = aModel.getObject();
        var fs = featureSupportRegistry.findExtension(feature).orElseThrow();
        featureState = Model.of(new FeatureState(VID.NONE_ID, feature, null));
        if (feature.getTagset() != null) {
            featureState.getObject().tagset = schemaService
                    .listTagsReorderable(feature.getTagset());
        }
        // We are adding only the focus component here because we do not want to display the label
        // which usually goes along with the feature editor. This assumes that there is a sensible
        // focus component... might not be the case for some multi-component editors.
        editor = fs.createEditor("value", this, null, null, featureState);
        editor.addFeatureUpdateBehavior();
        editor.getLabelComponent().setVisible(false);
        keyBindingsContainer.add(editor);

        keyBindingsContainer.add(createKeyBindingsList("keyBindings", keyBindings));
    }

    private ListView<KeyBinding> createKeyBindingsList(String aId,
            IModel<List<KeyBinding>> aKeyBindings)
    {
        return new ListView<KeyBinding>(aId, aKeyBindings)
        {
            private static final long serialVersionUID = 432136316377546825L;

            @Override
            protected void populateItem(ListItem<KeyBinding> aItem)
            {
                var feature = KeyBindingsConfigurationPanel.this.getModelObject();
                FeatureSupport<?> fs = featureSupportRegistry.findExtension(feature).orElseThrow();

                KeyBinding keyBinding = aItem.getModelObject();

                aItem.add(new KeybindingLabel("keyCombo", keyBinding));

                aItem.add(
                        new Label("value", fs.renderFeatureValue(feature, keyBinding.getValue())));
                aItem.add(new LambdaAjaxLink("removeKeyBinding",
                        _target -> removeKeyBinding(_target, aItem.getModelObject())));
            }
        };
    }

    public AnnotationFeature getModelObject()
    {
        return (AnnotationFeature) getDefaultModelObject();
    }

    private void addKeyBinding(AjaxRequestTarget aTarget, Form<KeyBinding> aForm)
    {
        var keyBinding = aForm.getModelObject();

        if (isBlank(keyBinding.getKeyCombo())) {
            error("Key combo is required");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        // Copy value from the value editor over into the form model (key binding) and then add it
        // to the list
        var feature = getModelObject();
        FeatureSupport<?> fs = featureSupportRegistry.findExtension(feature).orElseThrow();
        keyBinding.setValue(fs.unwrapFeatureValue(feature, featureState.getObject().value));
        keyBindings.getObject().add(keyBinding);

        // Clear form and value editor
        aForm.setModelObject(new KeyBinding());
        featureState.getObject().setValue(null);

        success("Key binding added. Do not forget to save the feature details!");
        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(keyBindingsContainer);
    }

    private void removeKeyBinding(AjaxRequestTarget aTarget, KeyBinding aBinding)
    {
        keyBindings.getObject().remove(aBinding);
        aTarget.add(keyBindingsContainer);
    }

    private static class KeyComboValidator
        implements IValidator<String>
    {
        private static final long serialVersionUID = 6697292531559511021L;

        @Override
        public void validate(IValidatable<String> aValidatable)
        {
            var keyCombo = aValidatable.getValue();
            var keyBinding = new KeyBinding(keyCombo, null);
            if (!keyBinding.isValid()) {
                aValidatable.error(new ValidationError("Invalid key combo: [" + keyCombo + "]"));
            }
        }
    }
}
