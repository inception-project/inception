/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
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

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxSubmitLink;
import wicket.contrib.input.events.key.KeyType;

/**
 * Can be added to a feature support traits editor to configure key bindings.
 */
public class KeyBindingsConfigurationPanel
    extends Panel
{
    private static final long serialVersionUID = -8294428032177255299L;

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    
    private final WebMarkupContainer keyBindingsContainer;
    private final IModel<List<KeyBinding>> keyBindings;
    
    private final FeatureEditor editor;
    private final IModel<FeatureState> featureState;

    public KeyBindingsConfigurationPanel(String aId, IModel<AnnotationFeature> aModel,
            IModel<List<KeyBinding>> aKeyBindings)
    {
        super(aId, aModel);
        
        keyBindings = aKeyBindings;
        
        Form<KeyBinding> keyBindingForm = new Form<>("keyBindingForm",
                CompoundPropertyModel.of(new KeyBinding()));
        add(keyBindingForm);

        keyBindingsContainer = new WebMarkupContainer("keyBindingsContainer");
        keyBindingsContainer.setOutputMarkupPlaceholderTag(true);
        keyBindingForm.add(keyBindingsContainer);

        keyBindingsContainer.add(new TextField<String>("keyCombo").add(new KeyComboValidator()));
        keyBindingsContainer.add(new LambdaAjaxSubmitLink<>("addKeyBinding", this::addKeyBinding));
        
        AnnotationFeature feature = aModel.getObject();
        FeatureSupport<?> fs = featureSupportRegistry.getFeatureSupport(feature);
        featureState = Model.of(new FeatureState(VID.NONE_ID, feature, null));
        // We are adding only the focus component here because we do not want to display the label
        // which usually goes along with the feature editor. This assumes that there is a sensible
        // focus component... might not be the case for some multi-component editors.
        editor = fs.createEditor("value", this, null, null, featureState);
        editor.getLabelComponent().setVisible(false);
        keyBindingsContainer.add(editor);
        
        ListView<KeyBinding> keyBindingsList = new ListView<KeyBinding>("keyBindings", keyBindings)
        {
            private static final long serialVersionUID = 432136316377546825L;

            @Override
            protected void populateItem(ListItem<KeyBinding> aItem)
            {
                KeyBinding keyBinding = aItem.getModelObject();
                
                try {
                    KeyType[] keyCombo = keyBinding.asKeyTypes();
                    String htmlKeyCombo = Arrays.stream(keyCombo)
                            .map(keyType -> keyType.getKeyCode().toUpperCase(Locale.US))
                            .collect(Collectors.joining(" ", "<kbd>", "</kbd>"));
                    aItem.add(new Label("keyCombo", htmlKeyCombo).setEscapeModelStrings(false));
                }
                catch (IllegalKeyComboException e) {
                    aItem.add(new Label("keyCombo", e.getMessage()));
                }

                aItem.add(
                        new Label("value", fs.renderFeatureValue(feature, keyBinding.getValue())));
                aItem.add(new LambdaAjaxLink("removeKeyBinding",
                    _target -> removeKeyBinding(_target, aItem.getModelObject())));
            }
        };
        keyBindingsContainer.add(keyBindingsList);    
    }
    
    public AnnotationFeature getModelObject()
    {
        return (AnnotationFeature) getDefaultModelObject();
    }
    
    private void addKeyBinding(AjaxRequestTarget aTarget, Form<KeyBinding> aForm)
    {
        // Copy value from the value editor over into the form model (key binding) and then add it
        // to the list
        AnnotationFeature feature = getModelObject();
        FeatureSupport<?> fs = featureSupportRegistry.getFeatureSupport(feature);
        KeyBinding keyBinding = aForm.getModelObject();
        keyBinding.setValue(fs.unwrapFeatureValue(feature, null, featureState.getObject().value));
        keyBindings.getObject().add(keyBinding);
        
        // Clear form and value editor
        aForm.setModelObject(new KeyBinding());
        featureState.getObject().setValue(null);
        
        aTarget.add(keyBindingsContainer);
    }
    
    private void removeKeyBinding(AjaxRequestTarget aTarget, KeyBinding aBinding)
    {
        keyBindings.getObject().remove(aBinding);
        aTarget.add(keyBindingsContainer);
    }
    
    private class KeyComboValidator
        implements IValidator<String>
    {
        private static final long serialVersionUID = 6697292531559511021L;
    
        @Override
        public void validate(IValidatable<String> aValidatable)
        {
            String keyCombo = aValidatable.getValue();
            KeyBinding keyBinding = new KeyBinding(keyCombo, null);
            try {
                keyBinding.asKeyTypes();
            }
            catch (IllegalKeyComboException e) {
                aValidatable.error(new ValidationError(e.getMessage()));
            }
        }
    }
}
