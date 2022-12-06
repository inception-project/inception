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
package de.tudarmstadt.ukp.inception.export;

import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayerReference;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.ValidationMode;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

/**
 * This class contains Utility methods that can be used in Project settings.
 */
public class LayerImportExportUtils
{
    public static String exportLayerToJson(AnnotationSchemaService aSchemaService,
            AnnotationLayer layer)
        throws IOException
    {
        var exLayers = new ArrayList<ExportedAnnotationLayer>();

        var exMainLayer = exportLayerDetails(null, null, layer, aSchemaService);
        exLayers.add(exMainLayer);

        // If the layer is attached to another layer, then we also have to export
        // that, otherwise we would be missing it during re-import.
        if (layer.getAttachType() != null) {
            var attachLayer = layer.getAttachType();
            var exAttachLayer = exportLayerDetails(null, null, attachLayer, aSchemaService);
            exMainLayer
                    .setAttachType(new ExportedAnnotationLayerReference(exAttachLayer.getName()));
            exLayers.add(exAttachLayer);
        }

        return JSONUtil.toPrettyJsonString(exLayers);
    }

    /**
     * Read Tag and Tag Description. A line has a tag name and a tag description separated by a TAB
     * 
     * @param aLineSeparatedTags
     *            the line.
     * @return the parsed line.
     */
    public static Map<String, String> getTagSetFromFile(String aLineSeparatedTags)
    {
        Map<String, String> tags = new LinkedHashMap<>();
        StringTokenizer st = new StringTokenizer(aLineSeparatedTags, "\n");
        while (st.hasMoreTokens()) {
            StringTokenizer stTag = new StringTokenizer(st.nextToken(), "\t");
            String tag = stTag.nextToken();
            String description;
            if (stTag.hasMoreTokens()) {
                description = stTag.nextToken();
            }
            else {
                description = tag;
            }
            tags.put(tag.trim(), description);
        }
        return tags;
    }

    @Deprecated
    private static void createTagSet(TagSet aTagSet, ExportedTagSet aExTagSet, Project aProject,
            AnnotationSchemaService aAnnotationService)
        throws IOException
    {
        aTagSet.setCreateTag(aExTagSet.isCreateTag());
        aTagSet.setDescription(aExTagSet.getDescription());
        aTagSet.setLanguage(aExTagSet.getLanguage());
        aTagSet.setName(aExTagSet.getName());
        aTagSet.setProject(aProject);
        aAnnotationService.createTagSet(aTagSet);

        for (ExportedTag exTag : aExTagSet.getTags()) {
            // do not duplicate tag
            if (aAnnotationService.existsTag(exTag.getName(), aTagSet)) {
                continue;
            }
            Tag tag = new Tag();
            tag.setDescription(exTag.getDescription());
            tag.setTagSet(aTagSet);
            tag.setName(exTag.getName());
            aAnnotationService.createTag(tag);
        }
    }

    @Deprecated
    private static void setLayer(AnnotationSchemaService aAnnotationService, AnnotationLayer aLayer,
            ExportedAnnotationLayer aExLayer, Project aProject)
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
        aAnnotationService.createOrUpdateLayer(aLayer);
    }

    public static AnnotationLayer importLayerFile(AnnotationSchemaService annotationService,
            User user, Project project, InputStream aIS)
        throws IOException
    {
        String text = IOUtils.toString(aIS, "UTF-8");

        ExportedAnnotationLayer[] exLayers = JSONUtil.getObjectMapper().readValue(text,
                ExportedAnnotationLayer[].class);

        // First import the layers but without setting the attach-layers/features
        Map<String, ExportedAnnotationLayer> exLayersMap = new HashMap<>();
        Map<String, AnnotationLayer> layersMap = new HashMap<>();
        for (ExportedAnnotationLayer exLayer : exLayers) {
            AnnotationLayer layer = createLayer(annotationService, project, exLayer);
            layersMap.put(layer.getName(), layer);
            exLayersMap.put(layer.getName(), exLayer);
        }

        // Second fill in the attach-layer and attach-feature information
        for (AnnotationLayer layer : layersMap.values()) {
            ExportedAnnotationLayer exLayer = exLayersMap.get(layer.getName());
            if (exLayer.getAttachType() != null) {
                layer.setAttachType(layersMap.get(exLayer.getAttachType().getName()));
            }
            if (exLayer.getAttachFeature() != null) {
                AnnotationLayer attachLayer = annotationService.findLayer(project,
                        exLayer.getAttachType().getName());
                AnnotationFeature attachFeature = annotationService
                        .getFeature(exLayer.getAttachFeature().getName(), attachLayer);
                layer.setAttachFeature(attachFeature);
            }
            annotationService.createOrUpdateLayer(layer);
        }

        return layersMap.get(exLayers[0].getName());
    }

    private static AnnotationLayer createLayer(AnnotationSchemaService annotationService,
            Project project, ExportedAnnotationLayer aExLayer)
        throws IOException
    {
        AnnotationLayer layer;

        if (annotationService.existsLayer(aExLayer.getName(), aExLayer.getType(), project)) {
            layer = annotationService.findLayer(project, aExLayer.getName());
            setLayer(annotationService, layer, aExLayer, project);
        }
        else {
            layer = new AnnotationLayer();
            setLayer(annotationService, layer, aExLayer, project);
        }

        for (ExportedAnnotationFeature exfeature : aExLayer.getFeatures()) {
            ExportedTagSet exTagset = exfeature.getTagSet();
            TagSet tagSet = null;
            if (exTagset != null && annotationService.existsTagSet(exTagset.getName(), project)) {
                tagSet = annotationService.getTagSet(exTagset.getName(), project);
                createTagSet(tagSet, exTagset, project, annotationService);
            }
            else if (exTagset != null) {
                tagSet = new TagSet();
                createTagSet(tagSet, exTagset, project, annotationService);
            }
            if (annotationService.existsFeature(exfeature.getName(), layer)) {
                AnnotationFeature feature = annotationService.getFeature(exfeature.getName(),
                        layer);
                feature.setTagset(tagSet);
                setFeature(annotationService, feature, exfeature, project);
                continue;
            }
            AnnotationFeature feature = new AnnotationFeature();
            feature.setLayer(layer);
            feature.setTagset(tagSet);
            setFeature(annotationService, feature, exfeature, project);
        }

        return layer;
    }

    @Deprecated
    private static void setFeature(AnnotationSchemaService aAnnotationService,
            AnnotationFeature aFeature, ExportedAnnotationFeature aExFeature, Project aProject)
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

        aAnnotationService.createFeature(aFeature);
    }

    @Deprecated
    public static ExportedAnnotationLayer exportLayerDetails(
            Map<AnnotationLayer, ExportedAnnotationLayer> aLayerToExLayer,
            Map<AnnotationFeature, ExportedAnnotationFeature> aFeatureToExFeature,
            AnnotationLayer aLayer, AnnotationSchemaService aAnnotationService)
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

        List<ExportedAnnotationFeature> exFeatures = new ArrayList<>();
        for (AnnotationFeature feature : aAnnotationService.listAnnotationFeature(aLayer)) {
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
                for (Tag tag : aAnnotationService.listTags(tagSet)) {
                    ExportedTag exTag = new ExportedTag();
                    exTag.setDescription(tag.getDescription());
                    exTag.setName(tag.getName());
                    exportedTags.add(exTag);
                }
                exTagSet.setTags(exportedTags);
                exFeature.setTagSet(exTagSet);
            }
            exFeatures.add(exFeature);
            if (aFeatureToExFeature != null) {
                aFeatureToExFeature.put(feature, exFeature);
            }
        }
        exLayer.setFeatures(exFeatures);
        return exLayer;
    }
}
