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
package de.tudarmstadt.ukp.inception.schema.exporters;

import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.COREFERENCE_RELATION_FEATURE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.COREFERENCE_TYPE_FEATURE;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeatureReference;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayerReference;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.ValidationMode;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link AnnotationSchemaServiceAutoConfiguration#layerExporter}.
 * </p>
 */
public class LayerExporter
    implements ProjectExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(LayerExporter.class);

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
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aStage)
    {
        var exLayers = new ArrayList<ExportedAnnotationLayer>();

        // Store map of layer and its equivalent exLayer so that the attach type is attached later
        var layerToExLayers = new HashMap<AnnotationLayer, ExportedAnnotationLayer>();

        // Store map of feature and its equivalent exFeature so that the attach feature is attached
        // later
        var featureToExFeatures = new HashMap<AnnotationFeature, ExportedAnnotationFeature>();
        for (var layer : annotationService.listAnnotationLayer(aRequest.getProject())) {
            exLayers.add(exportLayerDetails(layerToExLayers, featureToExFeatures, layer));
        }

        // add the attach-type and attach-feature to the exported layers and exported feature
        for (var layer : layerToExLayers.keySet()) {
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

        LOG.info("Exported [{}] layers for project [{}]", exLayers.size(),
                aRequest.getProject().getName());
    }

    @SuppressWarnings("deprecation")
    private ExportedAnnotationLayer exportLayerDetails(
            Map<AnnotationLayer, ExportedAnnotationLayer> aLayerToExLayer,
            Map<AnnotationFeature, ExportedAnnotationFeature> aFeatureToExFeature,
            AnnotationLayer aLayer)
    {
        var exLayer = new ExportedAnnotationLayer();
        // Allow limited backwards compatibility by exporting data that older versions used
        exLayer.setAllowStacking(aLayer.isAllowStacking());
        exLayer.setLockToTokenOffset(AnchoringMode.SINGLE_TOKEN.equals(aLayer.getAnchoringMode()));
        exLayer.setMultipleTokens(AnchoringMode.TOKENS.equals(aLayer.getAnchoringMode()));
        exLayer.setBuiltIn(aLayer.isBuiltIn());
        exLayer.setReadonly(aLayer.isReadonly());
        exLayer.setCrossSentence(aLayer.isCrossSentence());
        exLayer.setDescription(aLayer.getDescription());
        exLayer.setEnabled(aLayer.isEnabled());
        exLayer.setOverlapMode(aLayer.getOverlapMode());
        exLayer.setAnchoringMode(aLayer.getAnchoringMode());
        exLayer.setValidationMode(aLayer.getValidationMode());
        exLayer.setLinkedListBehavior(aLayer.isLinkedListBehavior());
        exLayer.setName(aLayer.getName());
        exLayer.setProjectId(aLayer.getProject().getId());
        exLayer.setType(aLayer.getType());
        exLayer.setUiName(aLayer.getUiName());
        exLayer.setTraits(aLayer.getTraits());

        if (aLayerToExLayer != null) {
            aLayerToExLayer.put(aLayer, exLayer);
        }

        // Export features
        var exFeatures = new ArrayList<ExportedAnnotationFeature>();
        for (var feature : annotationService.listAnnotationFeature(aLayer)) {
            var exFeature = exportFeatureDetails(feature);
            exFeatures.add(exFeature);

            if (aFeatureToExFeature != null) {
                aFeatureToExFeature.put(feature, exFeature);
            }
        }
        exLayer.setFeatures(exFeatures);

        return exLayer;
    }

    private ExportedAnnotationFeature exportFeatureDetails(AnnotationFeature feature)
    {
        var exFeature = new ExportedAnnotationFeature();
        exFeature.setDescription(feature.getDescription());
        exFeature.setEnabled(feature.isEnabled());
        exFeature.setRemember(feature.isRemember());
        exFeature.setRequired(feature.isRequired());
        exFeature.setHideUnconstraintFeature(feature.isHideUnconstraintFeature());
        exFeature.setName(feature.getName());
        exFeature.setProjectId(feature.getProject().getId());
        exFeature.setType(feature.getType());
        exFeature.setUiName(feature.getUiName());
        exFeature.setVisible(feature.isVisible());
        exFeature.setIncludeInHover(feature.isIncludeInHover());
        exFeature.setMultiValueMode(feature.getMultiValueMode());
        exFeature.setLinkMode(feature.getLinkMode());
        exFeature.setLinkTypeName(feature.getLinkTypeName());
        exFeature.setLinkTypeRoleFeatureName(feature.getLinkTypeRoleFeatureName());
        exFeature.setLinkTypeTargetFeatureName(feature.getLinkTypeTargetFeatureName());
        exFeature.setTraits(feature.getTraits());
        exFeature.setCuratable(feature.isCuratable());
        exFeature.setRank(feature.getRank());

        if (feature.getTagset() != null) {
            var tagSet = feature.getTagset();
            // We export only the name here as a stub. The actual tag set is exported by
            // the TagSetExporter.
            var exTagSet = new ExportedTagSet();
            exTagSet.setName(tagSet.getName());
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
    private void importLayers(Project aProject, ExportedProject aExProject) throws IOException
    {
        // Round 1: layers and features
        for (var exLayer : aExProject.getLayers()) {
            if (annotationService.existsLayer(exLayer.getName(), exLayer.getType(), aProject)) {
                var layer = annotationService.findLayer(aProject, exLayer.getName());
                importLayer(layer, exLayer, aProject);
                for (var exfeature : exLayer.getFeatures()) {
                    if (annotationService.existsFeature(exfeature.getName(), layer)) {
                        var feature = annotationService.getFeature(exfeature.getName(), layer);
                        importFeature(feature, exfeature, aProject);
                        continue;
                    }
                    var feature = new AnnotationFeature();
                    feature.setLayer(layer);
                    importFeature(feature, exfeature, aProject);
                }
            }
            else {
                var layer = new AnnotationLayer();
                importLayer(layer, exLayer, aProject);
                for (var exfeature : exLayer.getFeatures()) {
                    var feature = new AnnotationFeature();
                    feature.setLayer(layer);
                    importFeature(feature, exfeature, aProject);
                }
            }
        }

        // Round 2: attach-layers, attach-features
        for (var exLayer : aExProject.getLayers()) {
            if (exLayer.getAttachType() != null) {
                var layer = annotationService.findLayer(aProject, exLayer.getName());
                var attachLayer = annotationService.findLayer(aProject,
                        exLayer.getAttachType().getName());
                layer.setAttachType(attachLayer);
                if (exLayer.getAttachFeature() != null) {
                    var attachFeature = annotationService
                            .getFeature(exLayer.getAttachFeature().getName(), attachLayer);
                    layer.setAttachFeature(attachFeature);
                }
                annotationService.createOrUpdateLayer(layer);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void importLayer(AnnotationLayer aLayer, ExportedAnnotationLayer aExLayer,
            Project aProject)
        throws IOException
    {
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
        aLayer.setTraits(aExLayer.getTraits());
        annotationService.createOrUpdateLayer(aLayer);
    }

    private void importFeature(AnnotationFeature aFeature, ExportedAnnotationFeature aExFeature,
            Project aProject)
    {
        aFeature.setDescription(aExFeature.getDescription());
        aFeature.setEnabled(aExFeature.isEnabled());
        aFeature.setVisible(aExFeature.isVisible());
        aFeature.setIncludeInHover(aExFeature.isIncludeInHover());
        aFeature.setUiName(aExFeature.getUiName());
        aFeature.setProject(aProject);
        aFeature.setName(aExFeature.getName());
        var isItChainedLayer = CHAIN_TYPE.equals(aFeature.getLayer().getType());
        if (isItChainedLayer && (COREFERENCE_TYPE_FEATURE.equals(aExFeature.getName())
                || COREFERENCE_RELATION_FEATURE.equals(aExFeature.getName()))) {
            aFeature.setType(CAS.TYPE_NAME_STRING);
        }
        else if (Token._TypeName.equals(aFeature.getLayer().getName())
                && Token._FeatName_morph.equals(aExFeature.getName())
                && Lemma._TypeName.equals(aExFeature.getType())) {
            // See https://github.com/inception-project/inception/issues/3080
            aFeature.setType(MorphologicalFeatures._TypeName);
        }
        else {
            aFeature.setType(aExFeature.getType());
        }
        aFeature.setRemember(aExFeature.isRemember());
        aFeature.setRequired(aExFeature.isRequired());
        aFeature.setHideUnconstraintFeature(aExFeature.isHideUnconstraintFeature());
        aFeature.setMode(aExFeature.getMultiValueMode());
        aFeature.setLinkMode(aExFeature.getLinkMode());
        aFeature.setLinkTypeName(aExFeature.getLinkTypeName());
        aFeature.setLinkTypeRoleFeatureName(aExFeature.getLinkTypeRoleFeatureName());
        aFeature.setLinkTypeTargetFeatureName(aExFeature.getLinkTypeTargetFeatureName());
        aFeature.setTraits(aExFeature.getTraits());
        aFeature.setCuratable(aExFeature.isCuratable());
        aFeature.setRank(aExFeature.getRank());

        if (aExFeature.getTagSet() != null) {
            var tagset = annotationService.getTagSet(aExFeature.getTagSet().getName(), aProject);
            aFeature.setTagset(tagset);
        }

        annotationService.createFeature(aFeature);
    }
}
