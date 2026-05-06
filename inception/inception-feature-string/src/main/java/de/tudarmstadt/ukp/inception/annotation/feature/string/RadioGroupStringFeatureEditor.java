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
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.wicket.behavior.Behavior.onTag;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IModelComparator;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBinding;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBindingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeybindingLabel;
import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.inception.schema.api.feature.SuggestionStatePanel;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.inception.support.wicket.DescriptionTooltipBehavior.Mode;

public class RadioGroupStringFeatureEditor
    extends TextFeatureEditorBase
{
    private static final long serialVersionUID = 9112762779124263198L;

    private StringFeatureTraits traits;
    private KeyBindingsPanel keyBindingsPanel;

    public RadioGroupStringFeatureEditor(String aId, MarkupContainer aItem,
            IModel<FeatureState> aModel, AnnotationActionHandler aHandler)
    {
        super(aId, aItem, aModel);

        var feat = getModelObject().feature;
        traits = readFeatureTraits(feat);

        add(new LambdaAjaxLink("clear", this::actionClear));

        var emptyTagsetWarning = new WebMarkupContainer("emptyTagsetWarning");
        emptyTagsetWarning.setOutputMarkupPlaceholderTag(true);
        emptyTagsetWarning.add(visibleWhen(() -> CollectionUtils.isEmpty(getModelObject().tagset)));
        add(emptyTagsetWarning);

        keyBindingsPanel = new KeyBindingsPanel("keyBindings", () -> traits.getKeyBindings(),
                aModel, aHandler);
        // The key bindings are only visible when the label is also enabled, i.e. when the
        // editor is used in a "normal" context and not e.g. in the keybindings
        // configuration panel
        keyBindingsPanel.add(visibleWhen(() -> getLabelComponent().isVisible()));
        // We render the key bindings inside the choices already. The panel is only necessary to
        // actually react to the bindings
        keyBindingsPanel.setToggleVisibile(false);
        add(keyBindingsPanel);

        add(new SuggestionStatePanel("suggestionInfo", aModel));
    }

    private void actionClear(AjaxRequestTarget aTarget)
    {
        getModelObject().value = null;
        aTarget.add(this);
        send(getFocusComponent(), BUBBLE,
                new FeatureEditorValueChangedEvent(RadioGroupStringFeatureEditor.this, aTarget));
    }

    @Override
    protected FormComponent<?> createInputField()
    {
        var group = new RadioGroup<Object>("value", new PropertyModel<>(getModel(), "value"))
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

                        var tagA = a instanceof ReorderableTag ? ((ReorderableTag) a).getName()
                                : String.valueOf(a);
                        var tagB = b instanceof ReorderableTag ? ((ReorderableTag) b).getName()
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
                var tag = item.getModel();

                var radio = new Radio<>("radio", (IModel) tag, aGroup);
                item.add(radio);

                var button = new WebMarkupContainer("button", item.getModel());
                button.add(onTag((c, t) -> t.put("for", radio.getMarkupId())));
                item.add(button);

                var label = new Label("label", item.getModel().map(ReorderableTag::getName));
                label.add(AttributeModifier.append("class", item.getModel()
                        .map(ReorderableTag::getReordered).map(_flag -> _flag ? "fw-bold" : "")));
                button.add(label);

                var score = new Label("score", item.getModel().map(ReorderableTag::getScore));
                button.add(score);

                var descriptionText = item.getModel().getObject().getDescription();
                var description = new WebMarkupContainer("description");
                description.add(visibleWhen(() -> isNotBlank(descriptionText)));
                if (isNotBlank(descriptionText)) {
                    var tooltip = new DescriptionTooltipBehavior(
                            item.getModel().map(ReorderableTag::getName).getObject(),
                            descriptionText);
                    tooltip.setMode(Mode.MARKDOWN);
                    description.add(tooltip);
                }
                button.add(description);

                var keyComboModel = LoadableDetachableModel.of(() -> getKeyBinding(tag));
                var keyCombo = new KeybindingLabel("keyCombo", keyComboModel);
                keyCombo.add(visibleWhen(() -> keyBindingsPanel.isVisibilityAllowed()));
                button.add(keyCombo);
            }
        };
    }

    private KeyBinding getKeyBinding(IModel<ReorderableTag> aTag)
    {
        if (traits.getKeyBindings() == null) {
            return null;
        }

        return traits.getKeyBindings().stream() //
                .filter(kb -> aTag.map(t -> Objects.equals(t.getName(), kb.getValue()))
                        .orElse(false).getObject())
                .findFirst() //
                .orElse(null);
    }

    @Override
    public void addFeatureUpdateBehavior()
    {
        // Need to use a AjaxFormChoiceComponentUpdatingBehavior here since we use a RadioGroup
        // here.
        var focusComponent = getFocusComponent();
        focusComponent.add(new AjaxFormChoiceComponentUpdatingBehavior()
        {
            private static final long serialVersionUID = -5058365578109385064L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                send(focusComponent, BUBBLE, new FeatureEditorValueChangedEvent(
                        RadioGroupStringFeatureEditor.this, aTarget));
            }
        });
    }
}
