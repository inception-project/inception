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

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;

public class DocumentMetadataAnnotationDetailPanel extends Panel
{
    private static final Logger LOG = LoggerFactory
            .getLogger(DocumentMetadataAnnotationDetailPanel.class);
    
    public static final String ID_PREFIX = "metaFeatureEditorHead";
    
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    
    private final JCasProvider jcasProvider;
    private final IModel<Project> project;
    
    public DocumentMetadataAnnotationDetailPanel(String aId, IModel<VID> aModel,
            JCasProvider aJCasProvider, IModel<Project> aProject)
    {
        super(aId, aModel);
        
        jcasProvider = aJCasProvider;
        project = aProject;
        
        add(new ListView<FeatureState>("features", this::listFeatures)
        {
            private static final long serialVersionUID = -1139622234318691941L;

            @Override
            protected void populateItem(ListItem<FeatureState> item)
            {
                // Feature editors that allow multiple values may want to update themselves,
                // e.g. to add another slot.
                item.setOutputMarkupId(true);

                final FeatureState featureState = item.getModelObject();
                final FeatureEditor editor;
                
                // Look up a suitable editor and instantiate it
                FeatureSupport featureSupport = featureSupportRegistry
                        .getFeatureSupport(featureState.feature);
                editor = featureSupport.createEditor("editor",
                        DocumentMetadataAnnotationDetailPanel.this, null, aModel, item.getModel());

                if (!featureState.feature.getLayer().isReadonly()) {
                    addRefreshFeaturePanelBehavior(editor);

                    // Add tooltip on label
                    StringBuilder tooltipTitle = new StringBuilder();
                    tooltipTitle.append(featureState.feature.getUiName());
                    if (featureState.feature.getTagset() != null) {
                        tooltipTitle.append(" (");
                        tooltipTitle.append(featureState.feature.getTagset().getName());
                        tooltipTitle.append(')');
                    }

                    Component labelComponent = editor.getLabelComponent();
                    labelComponent.add(new AttributeAppender("style", "cursor: help", ";"));
                    labelComponent.add(new DescriptionTooltipBehavior(tooltipTitle.toString(),
                        featureState.feature.getDescription()));
                }
                else {
                    editor.getFocusComponent().setEnabled(false);
                }

                // We need to enable the markup ID here because we use it during the AJAX behavior
                // that automatically saves feature editors on change/blur. 
                // Check addAnnotateActionBehavior.
                editor.setOutputMarkupId(true);
                editor.setOutputMarkupPlaceholderTag(true);
                
                // Ensure that markup IDs of feature editor focus components remain constant 
                // across refreshes of the feature editor panel. This is required to restore the
                // focus.
                editor.getFocusComponent().setOutputMarkupId(true);
                editor.getFocusComponent().setMarkupId(
                        ID_PREFIX + editor.getModelObject().feature.getId());
                
                item.add(editor);            
            }
        });
    }
    
    public void setModelObject(VID aVID)
    {
        setDefaultModelObject(aVID);
    }
    
    public VID getModelObject()
    {
        return (VID) getDefaultModelObject();
    }

    private List<FeatureState> listFeatures()
    {
        VID vid = getModelObject();
        Project proj = project.getObject();
        
        if (proj == null || vid == null && vid.isNotSet()) {
            return emptyList();
        }
            
        JCas jcas;
        try {
            jcas = jcasProvider.get();
        }
        catch (IOException e) {
            LOG.error("Unable to load CAS", e);
            return emptyList();
        }
        
        AnnotationBaseFS fs = WebAnnoCasUtil.selectByAddr(jcas, AnnotationBaseFS.class,
                vid.getId());
        
        AnnotationLayer layer = annotationService.getLayer(proj, fs);
        TypeAdapter adapter = annotationService.getAdapter(layer);
        
        // Populate from feature structure
        List<FeatureState> featureStates = new ArrayList<>();
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(layer)) {
            if (!feature.isEnabled()) {
                continue;
            }

            Serializable value = null;
            if (fs != null) {
                value = adapter.getFeatureValue(feature, fs);
            }

            FeatureState featureState = new FeatureState(feature, value);
            featureStates.add(featureState);
            featureState.tagset = annotationService.listTags(featureState.feature.getTagset());
        }

        return featureStates;
    }
    
    private void addRefreshFeaturePanelBehavior(final FeatureEditor aFrag)
    {
        aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = 5179816588460867471L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                aTarget.add(DocumentMetadataAnnotationDetailPanel.this);
            }
        });
    }
}
