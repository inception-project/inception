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

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import wicket.contrib.input.events.EventType;

/**
 * This can be added to a feature editor component to enable keyboard shortcuts for it.
 */
public class KeyBindingsPanel
    extends Panel
{
    private static final long serialVersionUID = -3692080204613890753L;

    private final AnnotationActionHandler actionHandler;

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean AnnotationSchemaService schemaService;

    private boolean keyBindingsVisible = false;
    private boolean toggleVisibile = true;
    private IModel<List<KeyBinding>> keyBindings;

    public KeyBindingsPanel(String aId, IModel<List<KeyBinding>> aKeyBindings,
            IModel<FeatureState> aModel, AnnotationActionHandler aHandler)
    {
        super(aId, aModel);

        actionHandler = aHandler;
        keyBindings = aKeyBindings;

        add(visibleWhen(() -> keyBindings.map(List::isEmpty).getObject()));

        var keyBindingsContainer = new WebMarkupContainer("keyBindingsContainer");
        keyBindingsContainer.setOutputMarkupId(true);
        keyBindingsContainer.add(new ClassAttributeModifier()
        {
            private static final long serialVersionUID = -6661003854862017619L;

            @Override
            protected Set<String> update(Set<String> aClasses)
            {
                if (!keyBindingsVisible) {
                    aClasses.add("there-but-not-visible");
                }
                return aClasses;
            }
        });
        add(keyBindingsContainer);

        var showHideMessage = new Label("showHideMessage",
                () -> keyBindingsVisible ? "Hide key bindings..." : "Show key bindings...");
        showHideMessage.setOutputMarkupId(true);
        showHideMessage.add(visibleWhen(() -> toggleVisibile));

        var toggleKeyBindingHints = new LambdaAjaxLink("toggleKeyBindingHints", _target -> {
            keyBindingsVisible = !keyBindingsVisible;
            _target.add(keyBindingsContainer, showHideMessage);
        });
        toggleKeyBindingHints.add(showHideMessage);
        toggleKeyBindingHints
                .add(LambdaBehavior.visibleWhen(() -> !aKeyBindings.getObject().isEmpty()));
        add(toggleKeyBindingHints);

        keyBindingsContainer.add(new ListView<KeyBinding>("keyBindings", aKeyBindings)
        {
            private static final long serialVersionUID = 3942714328686353093L;

            @Override
            protected void populateItem(ListItem<KeyBinding> aItem)
            {
                var feature = aModel.getObject().feature;
                var value = aItem.getModelObject().getValue();
                var fs = featureSupportRegistry.findExtension(feature).orElseThrow();

                var link = new LambdaAjaxLink("shortcut",
                        _target -> actionInvokeShortcut(_target, aItem.getModelObject()));

                var keyCombo = aItem.getModelObject().asKeyTypes();
                link.add(new InputBehavior(keyCombo, EventType.click)
                {
                    private static final long serialVersionUID = -413804179695231212L;

                    @Override
                    protected Boolean getDisable_in_input()
                    {
                        return true;
                    }
                });

                aItem.add(new KeybindingLabel("keyCombo", aItem.getModel()));

                link.add(new Label("value", fs.renderFeatureValue(feature, value)));
                aItem.add(link);
            }
        });
    }

    public void setToggleVisibile(boolean aToggleVisibile)
    {
        toggleVisibile = aToggleVisibile;
    }

    public FeatureState getModelObject()
    {
        return (FeatureState) getDefaultModelObject();
    }

    private void actionInvokeShortcut(AjaxRequestTarget aTarget, KeyBinding aKeyBinding)
        throws IOException, AnnotationException
    {
        var cas = actionHandler.getEditorCas();
        var feature = getModelObject().feature;
        var fs = featureSupportRegistry.findExtension(feature).orElseThrow();
        getModelObject().value = fs.wrapFeatureValue(feature, cas, aKeyBinding.getValue());
        var featureEditor = findParent(FeatureEditor.class);
        send(featureEditor.getFocusComponent(), BUBBLE,
                new FeatureEditorValueChangedEvent(featureEditor, aTarget));
        aTarget.add(featureEditor);
    }
}
