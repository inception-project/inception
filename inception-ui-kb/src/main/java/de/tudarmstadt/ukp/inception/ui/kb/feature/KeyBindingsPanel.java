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
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.kb.KeyBinding;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;

public class KeyBindingsPanel
    extends Panel
{
    private static final long serialVersionUID = -3692080204613890753L;

    private final AnnotationActionHandler actionHandler;

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    public KeyBindingsPanel(String aId, IModel<List<KeyBinding>> aKeyBindings,
            IModel<FeatureState> aModel, AnnotationActionHandler aHandler)
    {
        super(aId, aModel);
        
        actionHandler = aHandler;
        
        add(new ListView<KeyBinding>("keyBindings", aKeyBindings) {
            private static final long serialVersionUID = 3942714328686353093L;

            @Override
            protected void populateItem(ListItem<KeyBinding> aItem)
            {
                AnnotationFeature feature = aModel.getObject().feature;
                String value = aItem.getModelObject().getValue();
                FeatureSupport<?> fs = featureSupportRegistry.getFeatureSupport(feature);
                
                LambdaAjaxLink link = new LambdaAjaxLink("shortcut",
                        _target -> actionInvokeShortcut(_target, aItem.getModelObject()));
                link.add(new InputBehavior(aItem.getModelObject().asKeyTypes(), EventType.click) {
                    private static final long serialVersionUID = -413804179695231212L;

                    @Override
                    protected Boolean getDisable_in_input()
                    {
                        return true;
                    }
                });
                aItem.add(link);
                link.add(new Label("value", fs.renderFeatureValue(feature, value)));
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
        FeatureSupport<?> fs = featureSupportRegistry.getFeatureSupport(feature);
        getModelObject().value = fs.wrapFeatureValue(feature, cas, aKeyBinding.getValue());
        actionHandler.actionCreateOrUpdate(aTarget, actionHandler.getEditorCas());
    }
}
