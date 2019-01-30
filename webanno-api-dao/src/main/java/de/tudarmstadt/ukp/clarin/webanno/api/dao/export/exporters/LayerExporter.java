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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.export.exporters;

import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeatureReference;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayerReference;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.ValidationMode;

@Component
public class LayerExporter
    implements ProjectExporter
{
    private final AnnotationSchemaService annotationService;
    
    @Autowired
    public LayerExporter(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public List<Class<? extends ProjectExporter>> getImportDependencies()
    {
        // Need to have the tagsets imported first so we can assign them to the features
        return asList(TagSetExporter.class);
    }
    
    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aStage)
        throws Exception
    {
        List<ExportedAnnotationLayer> exLayers = new ArrayList<>();
        
        // Store map of layer and its equivalent exLayer so that the attach type is attached later
        Map<AnnotationLayer, ExportedAnnotationLayer> layerToExLayers = new HashMap<>();
        
        // Store map of feature and its equivalent exFeature so that the attach feature is attached
        // later
        Map<AnnotationFeature, ExportedAnnotationFeature> featureToExFeatures = new HashMap<>();
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aRequest.getProject())) {
            exLayers.add(exportLayerDetails(layerToExLayers, featureToExFeatures, layer));
        }

        // add the attach-type and attach-feature to the exported layers and exported feature
        for (AnnotationLayer layer : layerToExLayers.keySet()) {
            if (layer.getAttachType() != null) {
                layerToExLayers.get(layer).setAttachType(
                        new ExportedAnnotationLayerReference(layer.getAttachType().getName()));
            }
            
            if (layer.getAttachFeature() != null) {
                layerToExLayers.get(layer).setAttachFeature(
                        new ExportedAnnotationFeatureReference(layer.getAttachFeature()));
            }
        }
        
        aExProject.setLayers(exLayers);
    }
    
    public ExportedAnnotationLayer exportLayerDetails(
            Map<AnnotationLayer, ExportedAnnotationLayer> aLayerToExLayer,
            Map<AnnotationFeature, ExportedAnnotationFeature> aFeatureToExFeature,
            AnnotationLayer aLayer)
    {
        ExportedAnnotationLayer exLayer = new ExportedAnnotationLayer();
        exLayer.setAllowStacking(aLayer.isAllowStacking());
        exLayer.setBuiltIn(aLayer.isBuiltIn());
        exLayer.setReadonly(aLayer.isReadonly());
        exLayer.setCrossSentence(aLayer.isCrossSentence());
        exLayer.setDescription(aLayer.getDescription());
        exLayer.setEnabled(aLayer.isEnabled());
        exLayer.setLockToTokenOffset(AnchoringMode.SINGLE_TOKEN.equals(aLayer.getAnchoringMode()));
        exLayer.setMultipleTokens(AnchoringMode.TOKENS.equals(aLayer.getAnchoringMode()));
        exLayer.setOverlapMode(aLayer.getOverlapMode());
        exLayer.setAnchoringMode(aLayer.getAnchoringMode());
        exLayer.setValidationMode(aLayer.getValidationMode());
        exLayer.setLinkedListBehavior(aLayer.isLinkedListBehavior());
        exLayer.setName(aLayer.getName());
        exLayer.setProjectName(aLayer.getProject().getName());
        exLayer.setType(aLayer.getType());
        exLayer.setUiName(aLayer.getUiName());

        if (aLayerToExLayer != null) {
            aLayerToExLayer.put(aLayer, exLayer);
        }

        // Export features
        List<ExportedAnnotationFeature> exFeatures = new ArrayList<>();
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(aLayer)) {
            ExportedAnnotationFeature exFeature = exportFeatureDetails(feature);
            exFeatures.add(exFeature);
            
            if (aFeatureToExFeature != null) {
                aFeatureToExFeature.put(feature, exFeature);
            }
        }
        exLayer.setFeatures(exFeatures);
        
        return exLayer;
    }    
    
    public ExportedAnnotationFeature exportFeatureDetails(AnnotationFeature feature)
    {
        ExportedAnnotationFeature exFeature = new ExportedAnnotationFeature();
        exFeature.setDescription(feature.getDescription());
        exFeature.setEnabled(feature.isEnabled());
        exFeature.setRemember(feature.isRemember());
        exFeature.setRequired(feature.isRequired());
        exFeature.setHideUnconstraintFeature(feature.isHideUnconstraintFeature());
        exFeature.setName(feature.getName());
        exFeature.setProjectName(feature.getProject().getName());
        exFeature.setType(feature.getType());
        exFeature.setUiName(feature.getUiName());
        exFeature.setVisible(feature.isVisible());
        exFeature.setMultiValueMode(feature.getMultiValueMode());
        exFeature.setLinkMode(feature.getLinkMode());
        exFeature.setLinkTypeName(feature.getLinkTypeName());
        exFeature.setLinkTypeRoleFeatureName(feature.getLinkTypeRoleFeatureName());
        exFeature.setLinkTypeTargetFeatureName(feature.getLinkTypeTargetFeatureName());
        exFeature.setTraits(feature.getTraits());
        
        if (feature.getTagset() != null) {
            TagSet tagSet = feature.getTagset();
            ExportedTagSet exTagSet = new ExportedTagSet();
            exTagSet.setDescription(tagSet.getDescription());
            exTagSet.setLanguage(tagSet.getLanguage());
            exTagSet.setName(tagSet.getName());
            exTagSet.setCreateTag(tagSet.isCreateTag());

            List<ExportedTag> exportedTags = new ArrayList<>();
            for (Tag tag : annotationService.listTags(tagSet)) {
                ExportedTag exTag = new ExportedTag();
                exTag.setDescription(tag.getDescription());
                exTag.setName(tag.getName());
                exportedTags.add(exTag);
            }
            exTagSet.setTags(exportedTags);
            exFeature.setTagSet(exTagSet);
        }
        
        return exFeature;
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        importLayers(aProject, aExProject);
    }
    
    /**
     * Create a {@link TagSet} for the imported project,
     * 
     * @param aProject
     *            a project.
     * @param aExProject
     *            the settings.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public void importLayers(Project aProject, ExportedProject aExProject) throws IOException
    {
        // Round 1: layers and features
        for (ExportedAnnotationLayer exLayer : aExProject.getLayers()) {
            if (annotationService.existsLayer(exLayer.getName(), exLayer.getType(), aProject)) {
                AnnotationLayer layer = annotationService.getLayer(exLayer.getName(), aProject);
                importLayer(layer, exLayer, aProject);
                for (ExportedAnnotationFeature exfeature : exLayer.getFeatures()) {
                    if (annotationService.existsFeature(exfeature.getName(), layer)) {
                        AnnotationFeature feature = annotationService.getFeature(
                                exfeature.getName(), layer);
                        importFeature(feature, exfeature, aProject);
                        continue;
                    }
                    AnnotationFeature feature = new AnnotationFeature();
                    feature.setLayer(layer);
                    importFeature(feature, exfeature, aProject);
                }
            }
            else {
                AnnotationLayer layer = new AnnotationLayer();
                importLayer(layer, exLayer, aProject);
                for (ExportedAnnotationFeature exfeature : exLayer.getFeatures()) {
                    AnnotationFeature feature = new AnnotationFeature();
                    feature.setLayer(layer);
                    importFeature(feature, exfeature, aProject);
                }
            }
        }
        
        // Round 2: attach-layers, attach-features
        for (ExportedAnnotationLayer exLayer : aExProject.getLayers()) {
            if (exLayer.getAttachType() != null) {
                AnnotationLayer layer = annotationService.getLayer(exLayer.getName(), aProject);
                AnnotationLayer attachLayer = annotationService.getLayer(exLayer.getAttachType()
                        .getName(), aProject);
                layer.setAttachType(attachLayer);
                if (exLayer.getAttachFeature() != null) {
                    AnnotationFeature attachFeature = annotationService
                            .getFeature(exLayer.getAttachFeature().getName(), attachLayer);
                    layer.setAttachFeature(attachFeature);
                }
                annotationService.createLayer(layer);
            }
        }
    }
    
    public void importLayer(AnnotationLayer aLayer, ExportedAnnotationLayer aExLayer,
            Project aProject)
        throws IOException
    {
        aLayer.setAllowStacking(aExLayer.isAllowStacking());
        aLayer.setBuiltIn(aExLayer.isBuiltIn());
        aLayer.setReadonly(aExLayer.isReadonly());
        aLayer.setCrossSentence(aExLayer.isCrossSentence());
        aLayer.setDescription(aExLayer.getDescription());
        aLayer.setEnabled(aExLayer.isEnabled());
        if (aExLayer.getAnchoringMode() == null) {
            // This allows importing old projects which did not have the anchoring mode yet
            aLayer.setAnchoringMode(aExLayer.isLockToTokenOffset(), aExLayer.isMultipleTokens());
        }
        else {
            aLayer.setAnchoringMode(aExLayer.getAnchoringMode());
        }
        if (aExLayer.getOverlapMode() == null) {
            // This allows importing old projects which did not have the overlap mode yet
            aLayer.setOverlapMode(aExLayer.isAllowStacking() ? ANY_OVERLAP : OVERLAP_ONLY);
        }
        else {
            aLayer.setOverlapMode(aExLayer.getOverlapMode());
        }
        aLayer.setValidationMode(aExLayer.getValidationMode() != null ? aExLayer.getValidationMode()
                : ValidationMode.NEVER);
        aLayer.setLinkedListBehavior(aExLayer.isLinkedListBehavior());
        aLayer.setUiName(aExLayer.getUiName());
        aLayer.setName(aExLayer.getName());
        aLayer.setProject(aProject);
        aLayer.setType(aExLayer.getType());
        annotationService.createLayer(aLayer);
    }

    public void importFeature(AnnotationFeature aFeature, ExportedAnnotationFeature aExFeature,
            Project aProject)
    {
        aFeature.setDescription(aExFeature.getDescription());
        aFeature.setEnabled(aExFeature.isEnabled());
        aFeature.setVisible(aExFeature.isVisible());
        aFeature.setUiName(aExFeature.getUiName());
        aFeature.setProject(aProject);
        aFeature.setLayer(aFeature.getLayer());
        boolean isItChainedLayer = aFeature.getLayer().getType().equals(WebAnnoConst.CHAIN_TYPE);
        if (isItChainedLayer && (aExFeature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)
                || aExFeature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE))) {
            aFeature.setType(CAS.TYPE_NAME_STRING);
        }
        else {
            aFeature.setType(aExFeature.getType());
        }
        aFeature.setName(aExFeature.getName());
        aFeature.setRemember(aExFeature.isRemember());
        aFeature.setRequired(aExFeature.isRequired());
        aFeature.setHideUnconstraintFeature(aExFeature.isHideUnconstraintFeature());
        aFeature.setMode(aExFeature.getMultiValueMode());
        aFeature.setLinkMode(aExFeature.getLinkMode());
        aFeature.setLinkTypeName(aExFeature.getLinkTypeName());
        aFeature.setLinkTypeRoleFeatureName(aExFeature.getLinkTypeRoleFeatureName());
        aFeature.setLinkTypeTargetFeatureName(aExFeature.getLinkTypeTargetFeatureName());
        aFeature.setTraits(aExFeature.getTraits());
        
        if (aExFeature.getTagSet() != null) {
            TagSet tagset = annotationService.getTagSet(aExFeature.getTagSet().getName(), aProject);
            aFeature.setTagset(tagset);
        }
        
        annotationService.createFeature(aFeature);
    }
}
