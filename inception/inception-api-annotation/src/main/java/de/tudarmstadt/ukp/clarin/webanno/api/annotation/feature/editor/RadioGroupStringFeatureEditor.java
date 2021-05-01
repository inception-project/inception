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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IModelComparator;
import org.apache.wicket.model.PropertyModel;

import com.googlecode.wicket.kendo.ui.form.Radio;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.event.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBindingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;

public class RadioGroupStringFeatureEditor
    extends TextFeatureEditorBase
{
    private static final long serialVersionUID = 9112762779124263198L;

    public RadioGroupStringFeatureEditor(String aId, MarkupContainer aItem,
            IModel<FeatureState> aModel, AnnotationActionHandler aHandler)
    {
        super(aId, aItem, aModel);

        AnnotationFeature feat = getModelObject().feature;
        StringFeatureTraits traits = readFeatureTraits(feat);

        add(new KeyBindingsPanel("keyBindings", () -> traits.getKeyBindings(), aModel, aHandler)
                // The key bindings are only visible when the label is also enabled, i.e. when the
                // editor is used in a "normal" context and not e.g. in the keybindings
                // configuration panel
                .add(visibleWhen(() -> getLabelComponent().isVisible())));
    }

    @Override
    protected FormComponent createInputField()
    {
        RadioGroup<Object> group = new RadioGroup<Object>("value",
                new PropertyModel<>(getModel(), "value"))
        {
            private static final long serialVersionUID = 1L;

            @Override
            public IModelComparator getModelComparator()
            {
                return new IModelComparator()
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean compare(Component component, Object b)
                    {
                        final Object a = component.getDefaultModelObject();
                        if (a == null && b == null) {
                            return true;
                        }
                        if (a == null || b == null) {
                            return false;
                        }

                        String tagA = a instanceof ReorderableTag ? ((ReorderableTag) a).getName()
                                : String.valueOf(a);
                        String tagB = b instanceof ReorderableTag ? ((ReorderableTag) b).getName()
                                : String.valueOf(b);

                        return tagA.equals(tagB);
                    }
                };
            }

            @Override
            public void convertInput()
            {
                super.convertInput();
                Object convertedInput = getConvertedInput();
                if (convertedInput instanceof ReorderableTag) {
                    setConvertedInput(((ReorderableTag) convertedInput).getName());
                }
            };
        };
        group.add(createFeaturesList(group, getModelObject().tagset));
        return group;
    }

    private ListView<ReorderableTag> createFeaturesList(RadioGroup<Object> aGroup,
            List<ReorderableTag> aOptions)
    {
        return new ListView<ReorderableTag>("radios", aOptions)
        {
            private static final long serialVersionUID = 6856342528153905386L;

            @Override
            protected void populateItem(ListItem<ReorderableTag> item)
            {
                Radio<Object> radio = new Radio<Object>("radio", (IModel) item.getModel(), aGroup);
                Radio.Label label = new Radio.Label("label",
                        item.getModel().map(ReorderableTag::getName), radio);
                item.add(radio, label);
            }
        };
    }

    @Override
    public void addFeatureUpdateBehavior()
    {
        // Need to use a AjaxFormChoiceComponentUpdatingBehavior here since we use a RadioGroup
        // here.
        FormComponent focusComponent = getFocusComponent();
        focusComponent.add(new AjaxFormChoiceComponentUpdatingBehavior()
        {
            private static final long serialVersionUID = -5058365578109385064L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes aAttributes)
            {
                super.updateAjaxAttributes(aAttributes);
                addDelay(aAttributes, 300);
            }

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                send(focusComponent, BUBBLE, new FeatureEditorValueChangedEvent(
                        RadioGroupStringFeatureEditor.this, aTarget));
            }
        });
    }
}
