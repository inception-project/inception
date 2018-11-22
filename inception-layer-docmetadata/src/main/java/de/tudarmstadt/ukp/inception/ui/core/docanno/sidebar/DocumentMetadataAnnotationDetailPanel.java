/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.ui.core.docanno.sidebar;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;

public class DocumentMetadataAnnotationDetailPanel extends Panel
{
    public static final String ID_PREFIX = "featureEditorHead";
    
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    
    public DocumentMetadataAnnotationDetailPanel(String aId, IModel<?> aModel)
    {
        super(aId, aModel);
        
//        add(new ListView<FeatureState>("features", this::listFeatures)
//        {
//            @Override
//            protected void populateItem(ListItem<FeatureState> item)
//            {
//                // Feature editors that allow multiple values may want to update themselves,
//                // e.g. to add another slot.
//                item.setOutputMarkupId(true);
//
//                final FeatureState featureState = item.getModelObject();
//                final FeatureEditor editor;
//                
//                // Look up a suitable editor and instantiate it
//                FeatureSupport featureSupport = featureSupportRegistry
//                        .getFeatureSupport(featureState.feature);
//                editor = featureSupport.createEditor("editor", AnnotationFeatureForm.this, 
//                        editorPanel, AnnotationFeatureForm.this.getModel(), item.getModel());
//
//                if (!featureState.feature.getLayer().isReadonly()) {
//                    AnnotatorState state = getModelObject();
//
//                    // Whenever it is updating an annotation, it updates automatically when a
//                    // component for the feature lost focus - but updating is for every component
//                    // edited LinkFeatureEditors must be excluded because the auto-update will 
//                    // break the ability to add slots. Adding a slot is NOT an annotation action.
//                    if (state.getSelection().getAnnotation().isSet()
//                        && !(editor instanceof LinkFeatureEditor)) {
//                        addAnnotateActionBehavior(editor);
//                    }
//                    else if (!(editor instanceof LinkFeatureEditor)) {
//                        addRefreshFeaturePanelBehavior(editor);
//                    }
//
//                    // Add tooltip on label
//                    StringBuilder tooltipTitle = new StringBuilder();
//                    tooltipTitle.append(featureState.feature.getUiName());
//                    if (featureState.feature.getTagset() != null) {
//                        tooltipTitle.append(" (");
//                        tooltipTitle.append(featureState.feature.getTagset().getName());
//                        tooltipTitle.append(')');
//                    }
//
//                    Component labelComponent = editor.getLabelComponent();
//                    labelComponent.add(new AttributeAppender("style", "cursor: help", ";"));
//                    labelComponent.add(new DescriptionTooltipBehavior(tooltipTitle.toString(),
//                        featureState.feature.getDescription()));
//                }
//                else {
//                    editor.getFocusComponent().setEnabled(false);
//                }
//
//                // We need to enable the markup ID here because we use it during the AJAX behavior
//                // that automatically saves feature editors on change/blur. 
//                // Check addAnnotateActionBehavior.
//                editor.setOutputMarkupId(true);
//                editor.setOutputMarkupPlaceholderTag(true);
//                
//                // Ensure that markup IDs of feature editor focus components remain constant 
//                // across refreshes of the feature editor panel. This is required to restore the
//                // focus.
//                editor.getFocusComponent().setOutputMarkupId(true);
//                editor.getFocusComponent().setMarkupId(
//                        ID_PREFIX + editor.getModelObject().feature.getId());
//                
//                item.add(editor);            
//            }
//        });
    }

//    private List<FeatureState> listFeatures();
//    {
//        return emptyList();
//    }
}
