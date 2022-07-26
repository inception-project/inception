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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.input.InputBehavior;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.key.KeyType;

/**
 * This can be added to a feature editor component to enable keyboard shortcuts for it.
 */
public class KeyBindingsPanel
    extends Panel
{
    private static final long serialVersionUID = -3692080204613890753L;

    private final AnnotationActionHandler actionHandler;

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    private boolean keyBindingsVisible = false;
    private IModel<List<KeyBinding>> keyBindings;

    public KeyBindingsPanel(String aId, IModel<List<KeyBinding>> aKeyBindings,
            IModel<FeatureState> aModel, AnnotationActionHandler aHandler)
    {
        super(aId, aModel);

        actionHandler = aHandler;
        keyBindings = aKeyBindings;

        add(visibleWhen(() -> keyBindings.map(List::isEmpty).getObject()));

        WebMarkupContainer keyBindingsContainer = new WebMarkupContainer("keyBindingsContainer");
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

        Label showHideMessage = new Label("showHideMessage", () -> {
            return keyBindingsVisible ? "Hide key bindings..." : "Show key bindings...";
        });
        showHideMessage.setOutputMarkupId(true);

        LambdaAjaxLink toggleKeyBindingHints = new LambdaAjaxLink("toggleKeyBindingHints",
                _target -> {
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
                AnnotationFeature feature = aModel.getObject().feature;
                String value = aItem.getModelObject().getValue();
                FeatureSupport<?> fs = featureSupportRegistry.findExtension(feature).orElseThrow();

                LambdaAjaxLink link = new LambdaAjaxLink("shortcut",
                        _target -> actionInvokeShortcut(_target, aItem.getModelObject()));

                KeyType[] keyCombo = aItem.getModelObject().asKeyTypes();
                link.add(new InputBehavior(keyCombo, EventType.click)
                {
                    private static final long serialVersionUID = -413804179695231212L;

                    @Override
                    protected Boolean getDisable_in_input()
                    {
                        return true;
                    }
                });

                aItem.add(new Label("keyCombo", aItem.getModelObject().asHtml())
                        .setEscapeModelStrings(false));

                link.add(new Label("value", fs.renderFeatureValue(feature, value)));
                aItem.add(link);
            }
        });
    }

    public FeatureState getModelObject()
    {
        return (FeatureState) getDefaultModelObject();
    }

    private void actionInvokeShortcut(AjaxRequestTarget aTarget, KeyBinding aKeyBinding)
        throws IOException, AnnotationException
    {
        CAS cas = actionHandler.getEditorCas();
        AnnotationFeature feature = getModelObject().feature;
        FeatureSupport<?> fs = featureSupportRegistry.findExtension(feature).orElseThrow();
        getModelObject().value = fs.wrapFeatureValue(feature, cas, aKeyBinding.getValue());
        actionHandler.actionCreateOrUpdate(aTarget, actionHandler.getEditorCas());
    }
}
